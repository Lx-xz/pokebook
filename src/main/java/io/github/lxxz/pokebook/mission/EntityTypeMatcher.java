package io.github.lxxz.pokebook.mission;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;

/** Casa com um tipo exato de entidade, ex.: {@code minecraft:cow}. */
public record EntityTypeMatcher(EntityType<?> type) implements TargetMatcher {
	@Override
	public boolean matches(Entity entity) {
		return entity.getType() == type;
	}

	@Override
	public Text describe() {
		return Text.translatable(type.getTranslationKey());
	}
}
