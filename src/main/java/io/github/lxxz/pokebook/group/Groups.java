package io.github.lxxz.pokebook.group;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.lxxz.pokebook.Pokebook;
import io.github.lxxz.pokebook.message.ContactDirectory;
import io.github.lxxz.pokebook.mission.Mission;
import io.github.lxxz.pokebook.mission.MissionProgress;
import io.github.lxxz.pokebook.mission.MissionService;
import io.github.lxxz.pokebook.mission.MissionTracker;
import io.github.lxxz.pokebook.mission.Missions;
import io.github.lxxz.pokebook.network.GroupMember;
import io.github.lxxz.pokebook.network.GroupPayload;
import io.github.lxxz.pokebook.network.MissionsUpdatePayload;
import io.github.lxxz.pokebook.notify.NotificationKind;
import io.github.lxxz.pokebook.notify.Notifications;
import io.github.lxxz.pokebook.ranking.Ranking;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Grupos, e o progresso das missões cooperativas.
 *
 * <p><b>Por que isto muda o modelo de dados.</b> Todo o resto do progresso é do jogador, num
 * anexo dele. Missão cooperativa tem <em>um</em> contador para várias pessoas — e várias
 * pessoas não cabem no save de nenhuma delas. O contador mora no grupo, e o grupo mora num
 * anexo do mundo principal, como a caixa de mensagens e o ranking.
 *
 * <p><b>O resgate continua pessoal.</b> Quando o grupo conclui, cada membro recebe a missão
 * como concluída <em>no próprio progresso</em> — e resgata a própria recompensa, no pokébook,
 * como qualquer outra. Isso resolve três coisas de uma vez: sair do grupo depois não tira de
 * ninguém o que já ganhou; entrar noutro grupo não deixa resgatar de novo, porque o
 * "resgatado" é seu; e o ranking e a aba social, que leem o progresso pessoal, contam sem
 * saber que grupos existem. Quem estava offline na conclusão fica com ela anotada no mundo, e
 * a recebe ao entrar.
 *
 * <p><b>Sem grupo, a missão cooperativa não anda.</b> É o que ela é. Ela aparece na lista
 * com a marca de grupo, para o jogador saber por que está parada.
 *
 * <p><b>Consentimento:</b> só se convida quem salvou você nos contatos, e o convite é aceito
 * por texto clicável no chat — o mesmo desenho do desafio de batalha. Quem sai leva nada: o
 * contador fica com o grupo. Um grupo que fica com uma pessoa só deixa de existir.
 */
public final class Groups {
	/** Quatro: o bastante para um grupo de amigos, pouco para virar uma fazenda de missões. */
	public static final int MAX_MEMBERS = 4;
	private static final int INVITE_EXPIRY_TICKS = 60 * 20;

	private record Group(List<UUID> members, Map<UUID, String> names, Map<Identifier, Integer> counts) {
		static final Codec<Group> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Uuids.CODEC.listOf().fieldOf("members").forGetter(Group::members),
			Codec.unboundedMap(Uuids.STRING_CODEC, Codec.STRING).optionalFieldOf("names", Map.of()).forGetter(Group::names),
			Codec.unboundedMap(Identifier.CODEC, Codec.INT).optionalFieldOf("counts", Map.of()).forGetter(Group::counts)
		).apply(instance, Group::new));

		Group {
			members = List.copyOf(members);
			names = Map.copyOf(names);
			counts = Map.copyOf(counts);
		}

		int count(Identifier mission) {
			return counts.getOrDefault(mission, 0);
		}
	}

	/** Todos os grupos, e as conclusões que esperam alguém entrar para serem entregues. */
	private record Registry(Map<UUID, Group> groups, Map<UUID, List<Identifier>> owed) {
		static final Codec<Registry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(Uuids.STRING_CODEC, Group.CODEC).optionalFieldOf("groups", Map.of()).forGetter(Registry::groups),
			Codec.unboundedMap(Uuids.STRING_CODEC, Identifier.CODEC.listOf()).optionalFieldOf("owed", Map.of()).forGetter(Registry::owed)
		).apply(instance, Registry::new));

		static final Registry EMPTY = new Registry(Map.<UUID, Group>of(), Map.<UUID, List<Identifier>>of());

		Registry {
			groups = Map.copyOf(groups);
			owed = Map.copyOf(owed);
		}
	}

	private static final AttachmentType<Registry> REGISTRY = AttachmentRegistry.create(
		Identifier.of(Pokebook.MOD_ID, "groups"),
		builder -> builder
			.initializer(() -> Registry.EMPTY)
			.persistent(Registry.CODEC)
	);

	private record Invite(UUID inviter, int createdAt) {
	}

	/** Convites esperando resposta, por quem foi convidado. Um por pessoa de cada vez. */
	private static final Map<UUID, Invite> PENDING = new HashMap<>();

	private Groups() {
	}

	public static void register() {
	}

	// ------------------------------------------------------------------ leitura

	private static Registry registry(MinecraftServer server) {
		return server.getOverworld().getAttachedOrCreate(REGISTRY);
	}

	private static void save(MinecraftServer server, Map<UUID, Group> groups, Map<UUID, List<Identifier>> owed) {
		ServerWorld overworld = server.getOverworld();
		overworld.setAttached(REGISTRY, new Registry(groups, owed));
	}

	/** O id do grupo em que este jogador está. São poucos grupos; percorrer é barato. */
	private static Optional<UUID> groupIdOf(MinecraftServer server, UUID player) {
		for (Map.Entry<UUID, Group> entry : registry(server).groups().entrySet()) {
			if (entry.getValue().members().contains(player)) {
				return Optional.of(entry.getKey());
			}
		}
		return Optional.empty();
	}

	private static Optional<Group> groupOf(MinecraftServer server, UUID player) {
		return groupIdOf(server, player).map(id -> registry(server).groups().get(id));
	}

	/**
	 * Quanto desta missão aparece para o jogador. Cooperativa: o que o grupo fez — ou
	 * completa, se ele já a concluiu por algum grupo.
	 */
	public static int count(ServerPlayerEntity player, Mission mission, MissionProgress progress) {
		if (!mission.cooperative() || progress.isComplete(mission)) {
			return progress.count(mission.id());
		}
		return groupOf(player.server, player.getUuid()).map(group -> group.count(mission.id())).orElse(0);
	}

	// ------------------------------------------------------------------ progresso

	/**
	 * Uma ação de um membro conta para uma missão cooperativa. Chamado pelo
	 * {@code MissionTracker} quando o alvo já bateu.
	 */
	public static void advance(ServerPlayerEntity player, Mission mission) {
		MinecraftServer server = player.server;
		Optional<UUID> groupId = groupIdOf(server, player.getUuid());
		if (groupId.isEmpty()) {
			return;
		}
		Registry registry = registry(server);
		Group group = registry.groups().get(groupId.get());
		int current = group.count(mission.id());
		if (current >= mission.required()) {
			return;
		}

		Map<Identifier, Integer> counts = new HashMap<>(group.counts());
		counts.put(mission.id(), current + 1);
		Map<UUID, Group> groups = new HashMap<>(registry.groups());
		groups.put(groupId.get(), new Group(group.members(), group.names(), counts));
		Map<UUID, List<Identifier>> owed = new HashMap<>(registry.owed());

		boolean completed = current + 1 >= mission.required();
		if (completed) {
			// Offline não se pode tocar no anexo de ninguém: fica anotado para a entrada.
			for (UUID member : group.members()) {
				if (server.getPlayerManager().getPlayer(member) == null) {
					List<Identifier> list = new ArrayList<>(owed.getOrDefault(member, List.of()));
					if (!list.contains(mission.id())) {
						list.add(mission.id());
					}
					owed.put(member, list);
				}
			}
		}
		save(server, groups, owed);

		for (UUID member : group.members()) {
			ServerPlayerEntity online = server.getPlayerManager().getPlayer(member);
			if (online == null) {
				continue;
			}
			if (completed) {
				grant(online, mission);
			}
			ServerPlayNetworking.send(online, new MissionsUpdatePayload(MissionService.snapshot(online)));
		}
	}

	/** Dá a missão como concluída no progresso pessoal, e avisa. */
	private static void grant(ServerPlayerEntity player, Mission mission) {
		MissionProgress progress = player.getAttachedOrCreate(MissionTracker.PROGRESS);
		if (!progress.fill(mission)) {
			return;
		}
		// O anexo só é marcado como sujo ao ser reatribuído.
		player.setAttached(MissionTracker.PROGRESS, progress);
		Notifications.send(player, NotificationKind.MISSION,
			Text.translatable("notification.pokebook.mission.group_completed"), mission.title());
		Ranking.update(player);
	}

	/** Na entrada: entrega o que o grupo concluiu enquanto ele estava fora, e anota o nome. */
	public static void onJoin(ServerPlayerEntity player) {
		MinecraftServer server = player.server;
		Registry registry = registry(server);
		List<Identifier> owed = registry.owed().get(player.getUuid());
		Map<UUID, Group> groups = new HashMap<>(registry.groups());
		boolean changed = false;

		Optional<UUID> groupId = groupIdOf(server, player.getUuid());
		String name = player.getGameProfile().getName();
		if (groupId.isPresent()) {
			Group group = groups.get(groupId.get());
			if (!name.equals(group.names().get(player.getUuid()))) {
				Map<UUID, String> names = new HashMap<>(group.names());
				names.put(player.getUuid(), name);
				groups.put(groupId.get(), new Group(group.members(), names, group.counts()));
				changed = true;
			}
		}

		Map<UUID, List<Identifier>> owedMap = registry.owed();
		if (owed != null) {
			for (Identifier id : owed) {
				Mission mission = Missions.byId(id);
				if (mission != null) {
					grant(player, mission);
				}
			}
			owedMap = new HashMap<>(registry.owed());
			owedMap.remove(player.getUuid());
			changed = true;
		}
		if (changed) {
			save(server, groups, owedMap);
		}
		if (owed != null) {
			ServerPlayNetworking.send(player, new MissionsUpdatePayload(MissionService.snapshot(player)));
		}
	}

	// ------------------------------------------------------------------ o grupo

	/** Manda a lista de membros a quem pediu. */
	public static void send(ServerPlayerEntity player) {
		List<GroupMember> members = new ArrayList<>();
		groupOf(player.server, player.getUuid()).ifPresent(group -> {
			for (UUID member : group.members()) {
				ServerPlayerEntity online = player.server.getPlayerManager().getPlayer(member);
				String name = online != null ? online.getGameProfile().getName() : group.names().getOrDefault(member, "?");
				members.add(new GroupMember(member, name, online != null));
			}
		});
		ServerPlayNetworking.send(player, new GroupPayload(members));
	}

	/** Todos os membros online ficam sabendo do grupo como ficou, e das missões. */
	private static void broadcast(MinecraftServer server, List<UUID> members) {
		for (UUID member : members) {
			ServerPlayerEntity online = server.getPlayerManager().getPlayer(member);
			if (online != null) {
				send(online);
				ServerPlayNetworking.send(online, new MissionsUpdatePayload(MissionService.snapshot(online)));
			}
		}
	}

	public static void invite(ServerPlayerEntity inviter, UUID targetId) {
		MinecraftServer server = inviter.server;
		ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetId);
		if (target == null || target == inviter) {
			inviter.sendMessage(Text.translatable("message.pokebook.group.offline"), true);
			return;
		}
		String targetName = target.getGameProfile().getName();
		// Consentimento de quem recebe: só convida quem já te salvou.
		if (!ContactDirectory.hasSaved(server, targetId, inviter.getUuid())) {
			inviter.sendMessage(Text.translatable("message.pokebook.group.not_accepting", targetName), false);
			return;
		}
		if (groupIdOf(server, targetId).isPresent()) {
			inviter.sendMessage(Text.translatable("message.pokebook.group.already", targetName), false);
			return;
		}
		if (groupOf(server, inviter.getUuid()).map(group -> group.members().size() >= MAX_MEMBERS).orElse(false)) {
			inviter.sendMessage(Text.translatable("message.pokebook.group.full", MAX_MEMBERS), false);
			return;
		}
		if (PENDING.containsKey(targetId)) {
			inviter.sendMessage(Text.translatable("message.pokebook.group.busy", targetName), false);
			return;
		}

		PENDING.put(targetId, new Invite(inviter.getUuid(), server.getTicks()));
		String inviterName = inviter.getGameProfile().getName();
		inviter.sendMessage(Text.translatable("message.pokebook.group.sent", targetName), false);
		target.sendMessage(Text.translatable("message.pokebook.group.received", inviterName,
			button("message.pokebook.group.accept", Formatting.GREEN, "/pokebook grupo aceitar " + inviterName),
			button("message.pokebook.group.decline", Formatting.RED, "/pokebook grupo recusar " + inviterName)), false);
		Notifications.send(target, NotificationKind.CONTACT,
			Text.translatable("notification.pokebook.group.title"),
			Text.translatable("notification.pokebook.group.body", inviterName));
	}

	private static MutableText button(String key, Formatting color, String command) {
		return Texts.bracketed(Text.translatable(key)).styled(style -> style
			.withColor(color)
			.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
			.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(command))));
	}

	/** Aceita o convite que este jogador recebeu daquele. Vem do comando que o clique roda. */
	public static void accept(ServerPlayerEntity invited, String inviterName) {
		ServerPlayerEntity inviter = take(invited, inviterName);
		if (inviter == null) {
			invited.sendMessage(Text.translatable("message.pokebook.group.none", inviterName), false);
			return;
		}
		MinecraftServer server = invited.server;
		// Tudo de novo: entre o convite e o aceite, qualquer um dos dois pode ter mudado.
		if (groupIdOf(server, invited.getUuid()).isPresent()) {
			invited.sendMessage(Text.translatable("message.pokebook.group.leave_first"), false);
			return;
		}
		Registry registry = registry(server);
		Map<UUID, Group> groups = new HashMap<>(registry.groups());
		Optional<UUID> groupId = groupIdOf(server, inviter.getUuid());
		UUID id;
		Group updated;
		if (groupId.isPresent()) {
			Group group = groups.get(groupId.get());
			if (group.members().size() >= MAX_MEMBERS) {
				invited.sendMessage(Text.translatable("message.pokebook.group.full", MAX_MEMBERS), false);
				return;
			}
			List<UUID> members = new ArrayList<>(group.members());
			members.add(invited.getUuid());
			Map<UUID, String> names = new HashMap<>(group.names());
			names.put(invited.getUuid(), invited.getGameProfile().getName());
			id = groupId.get();
			updated = new Group(members, names, group.counts());
		} else {
			// O primeiro aceite cria o grupo, que nasce com o contador zerado.
			id = UUID.randomUUID();
			updated = new Group(List.of(inviter.getUuid(), invited.getUuid()),
				Map.of(inviter.getUuid(), inviter.getGameProfile().getName(),
					invited.getUuid(), invited.getGameProfile().getName()),
				Map.of());
		}
		groups.put(id, updated);
		save(server, groups, registry.owed());

		Text joined = Text.translatable("message.pokebook.group.joined", invited.getGameProfile().getName());
		for (UUID member : updated.members()) {
			ServerPlayerEntity online = server.getPlayerManager().getPlayer(member);
			if (online != null) {
				online.sendMessage(joined, false);
			}
		}
		broadcast(server, updated.members());
	}

	public static void decline(ServerPlayerEntity invited, String inviterName) {
		ServerPlayerEntity inviter = take(invited, inviterName);
		if (inviter != null) {
			inviter.sendMessage(Text.translatable("message.pokebook.group.declined",
				invited.getGameProfile().getName()), false);
		}
	}

	/** O nome vem de um comando que qualquer um digita; quem autoriza é o registro de espera. */
	private static ServerPlayerEntity take(ServerPlayerEntity invited, String inviterName) {
		Invite invite = PENDING.get(invited.getUuid());
		if (invite == null) {
			return null;
		}
		ServerPlayerEntity inviter = invited.server.getPlayerManager().getPlayer(invite.inviter());
		if (inviter == null || !inviter.getGameProfile().getName().equalsIgnoreCase(inviterName)) {
			return null;
		}
		PENDING.remove(invited.getUuid());
		return inviter;
	}

	public static void leave(ServerPlayerEntity player) {
		MinecraftServer server = player.server;
		Optional<UUID> groupId = groupIdOf(server, player.getUuid());
		if (groupId.isEmpty()) {
			return;
		}
		Registry registry = registry(server);
		Map<UUID, Group> groups = new HashMap<>(registry.groups());
		Group group = groups.get(groupId.get());
		List<UUID> members = new ArrayList<>(group.members());
		members.remove(player.getUuid());
		Map<UUID, String> names = new HashMap<>(group.names());
		names.remove(player.getUuid());

		// Um grupo de uma pessoa só não coopera com ninguém.
		if (members.size() < 2) {
			groups.remove(groupId.get());
		} else {
			groups.put(groupId.get(), new Group(members, names, group.counts()));
		}
		save(server, groups, registry.owed());

		Text left = Text.translatable(members.size() < 2 ? "message.pokebook.group.dissolved" : "message.pokebook.group.left",
			player.getGameProfile().getName());
		for (UUID member : members) {
			ServerPlayerEntity online = server.getPlayerManager().getPlayer(member);
			if (online != null) {
				online.sendMessage(left, false);
			}
		}
		player.sendMessage(Text.translatable("message.pokebook.group.you_left"), true);
		List<UUID> everyone = new ArrayList<>(members);
		everyone.add(player.getUuid());
		broadcast(server, everyone);
	}

	/** Convite sem resposta por um minuto expira. */
	public static void tick(MinecraftServer server) {
		if (PENDING.isEmpty() || server.getTicks() % 20 != 0) {
			return;
		}
		Set<UUID> expired = new HashSet<>();
		PENDING.forEach((target, invite) -> {
			if (server.getTicks() - invite.createdAt() >= INVITE_EXPIRY_TICKS) {
				expired.add(target);
			}
		});
		for (UUID target : expired) {
			Invite invite = PENDING.remove(target);
			ServerPlayerEntity inviter = server.getPlayerManager().getPlayer(invite.inviter());
			if (inviter != null) {
				inviter.sendMessage(Text.translatable("message.pokebook.group.expired"), false);
			}
		}
	}

	/** Quem sai do servidor leva os convites em que está. O grupo, não: ele continua. */
	public static void disconnect(ServerPlayerEntity player) {
		PENDING.remove(player.getUuid());
		PENDING.values().removeIf(invite -> invite.inviter().equals(player.getUuid()));
	}
}
