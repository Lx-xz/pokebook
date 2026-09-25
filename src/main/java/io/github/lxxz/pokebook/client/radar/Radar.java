package io.github.lxxz.pokebook.client.radar;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * O radar de Pokémon: o que há por perto, a que distância e para que lado.
 *
 * <p><b>Vive na {@code main}, e não na branch do Cobblemon</b>, pelo mesmo motivo dos sons
 * emprestados: não menciona classe nenhuma dele. Um Pokémon é reconhecido pelo <b>id do
 * tipo de entidade</b> — {@code cobblemon:pokemon} — procurado no registro, e o nome vem do
 * {@code getName()} do vanilla, que o Cobblemon responde com a espécie (ou o apelido). Sem
 * o Cobblemon instalado o id não existe, nada casa, e o app nem aparece.
 *
 * <p><b>Só enxerga o que o cliente já enxerga.</b> O servidor manda ao cliente as entidades
 * num raio em volta dele — é a distância de rastreio de entidades — e é nelas que o radar
 * procura. Não há pacote novo, e por isso também não há como o radar revelar o que o
 * jogador não poderia ver chegando perto. Qualquer cliente modificado já vê isto; o radar só
 * organiza.
 *
 * <p>Não mostra se é brilhante nem se é lendário: isso exigiria ler dados da classe do
 * Pokémon, que é justamente o que mantém isto fora da {@code main} — e o próprio
 * levantamento de ideias apontava esse detalhe como o que desequilibra um radar. O dono do
 * servidor desliga o radar inteiro em {@code config/pokebook-server.json}.
 */
public final class Radar {
	/** O tipo de entidade do Pokémon no Cobblemon. Um id em texto — mencioná-lo não é depender dele. */
	public static final Identifier POKEMON = Identifier.of("cobblemon", "pokemon");

	/** Alcance do radar, em blocos. Além disso o servidor raramente manda entidades mesmo. */
	public static final double RANGE = 64.0;

	/** Oito setas, a partir de "em frente" e girando para a direita. */
	private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

	/**
	 * Um Pokémon no radar.
	 *
	 * @param entityId para a seta do HUD poder segui-lo enquanto ele andar
	 * @param relativeYaw para que lado, em graus: 0 em frente, negativo à esquerda
	 */
	public record Blip(int entityId, Text name, double distance, float relativeYaw) {
		public String arrow() {
			return ARROWS[Math.floorMod(Math.round(relativeYaw / 45f), ARROWS.length)];
		}
	}

	private Radar() {
	}

	/** Há como ter radar nesta instalação? */
	public static boolean available() {
		return FabricLoader.getInstance().isModLoaded(POKEMON.getNamespace());
	}

	/** Os Pokémon ao alcance, do mais perto para o mais longe. */
	public static List<Blip> scan(MinecraftClient client) {
		List<Blip> blips = new ArrayList<>();
		if (client.world == null || client.player == null) {
			return blips;
		}
		Vec3d eye = client.player.getPos();
		for (Entity entity : client.world.getEntities()) {
			if (!POKEMON.equals(Registries.ENTITY_TYPE.getId(entity.getType()))) {
				continue;
			}
			double distance = eye.distanceTo(entity.getPos());
			if (distance > RANGE) {
				continue;
			}
			// Mesma conta de ângulo da seta do HUD — ver PokebookHud.renderNavigation.
			double dx = entity.getX() - eye.x;
			double dz = entity.getZ() - eye.z;
			float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
			float relative = MathHelper.wrapDegrees(targetYaw - client.player.getYaw());
			blips.add(new Blip(entity.getId(), entity.getName(), distance, relative));
		}
		blips.sort(Comparator.comparingDouble(Blip::distance));
		return blips;
	}
}
