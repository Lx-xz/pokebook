package io.github.lxxz.pokebook.mission;

import com.mojang.serialization.Codec;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

/** Casa com um tipo exato de entidade, ex.: {@code minecraft:cow}. */
public record EntityTypeMatcher(EntityType<?> type) implements TargetMatcher {
	public static final Codec<EntityTypeMatcher> CODEC =
		Registries.ENTITY_TYPE.getCodec().xmap(EntityTypeMatcher::new, EntityTypeMatcher::type);

	@Override
	public boolean matches(MissionTarget target) {
		Entity entity = target.entity();
		return entity != null && entity.getType() == type;
	}

	@Override
	public Text describe() {
		return Text.translatable(type.getTranslationKey());
	}
}
