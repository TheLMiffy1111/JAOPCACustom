package thelm.jaopca.custom.library;

import net.minecraft.block.SoundType;

public enum EnumSoundType {

	ANVIL(SoundType.ANVIL), CLOTH(SoundType.CLOTH), GLASS(SoundType.GLASS), GROUND(SoundType.GROUND),
	LADDER(SoundType.LADDER), METAL(SoundType.METAL), PLANT(SoundType.PLANT), SAND(SoundType.SAND),
	SLIME(SoundType.SLIME), SNOW(SoundType.SNOW), STONE(SoundType.STONE), WOOD(SoundType.WOOD);

	SoundType soundType;

	EnumSoundType(SoundType soundType) {
		this.soundType = soundType;
	}

	public static SoundType fromName(String name) {
		for(EnumSoundType soundType : values()) {
			if(soundType.name().equalsIgnoreCase(name)) {
				return soundType.soundType;
			}
		}
		return SoundType.STONE;
	}
}
