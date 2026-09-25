package io.github.lxxz.pokebook.phone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Os ajustes do aparelho.
 *
 * <p>Vivem no servidor, e não num arquivo do cliente como os favoritos viviam, porque dois
 * deles são <b>regra</b>, não aparência: o "não perturbe" e a política de ligação decidem
 * se o telefone de alguém toca, e quem liga é outro cliente. Só o papel de parede é pura
 * aparência — mas separá-lo num segundo lugar de guarda seria complicar para economizar
 * nada.
 *
 * @param doNotDisturb bloqueia ligações, silencia notificações e suspende o
 *                     compartilhamento de localização — o "modo avião"
 * @param wallpaper índice em {@link #WALLPAPERS}; 0 é a tela sem papel de parede
 */
public record PhoneSettings(boolean doNotDisturb, CallPolicy callPolicy, int wallpaper) {
	/**
	 * Os papéis de parede, como cor ARGB pintada por cima da área acesa.
	 *
	 * <p><b>Todos claros, e é de propósito.</b> A paleta de texto da interface foi escolhida
	 * para fundo claro — ver {@code PokebookScreenBase}. Um papel de parede escuro deixaria
	 * o texto preto ilegível. O alfa baixo deixa a cor da tela aparecer por baixo, então o
	 * resultado é um tom, não uma troca.
	 *
	 * <p>O primeiro é transparente: é "sem papel de parede", o padrão.
	 */
	public static final List<Integer> WALLPAPERS = List.of(
		0x00000000,
		0x60FFB3C1,
		0x60FFD59E,
		0x60FFF3A0,
		0x60B8F2A6,
		0x60A8DCFF,
		0x60D4B8FF
	);

	public static final PhoneSettings DEFAULT = new PhoneSettings(false, CallPolicy.EVERYONE, 0);

	public static final Codec<PhoneSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("do_not_disturb", false).forGetter(PhoneSettings::doNotDisturb),
		CallPolicy.CODEC.optionalFieldOf("call_policy", CallPolicy.EVERYONE).forGetter(PhoneSettings::callPolicy),
		Codec.INT.optionalFieldOf("wallpaper", 0).forGetter(PhoneSettings::wallpaper)
	).apply(instance, PhoneSettings::new));

	/** A cor do papel de parede, tolerando índice fora da lista (de uma versão futura, por ex.). */
	public int wallpaperColor() {
		return wallpaper >= 0 && wallpaper < WALLPAPERS.size() ? WALLPAPERS.get(wallpaper) : 0;
	}

	/** Os mesmos ajustes com o que o cliente não decide sozinho posto dentro dos limites. */
	public PhoneSettings sanitized() {
		int safeWallpaper = wallpaper >= 0 && wallpaper < WALLPAPERS.size() ? wallpaper : 0;
		return new PhoneSettings(doNotDisturb, callPolicy, safeWallpaper);
	}
}
