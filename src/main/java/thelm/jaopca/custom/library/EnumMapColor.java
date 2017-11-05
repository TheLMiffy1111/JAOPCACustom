package thelm.jaopca.custom.library;

import net.minecraft.block.material.MapColor;

public enum EnumMapColor {
	
	AIR, GRASS, SAND, CLOTH, TNT, ICE, IRON, FOLIAGE, SNOW, CLAY, DIRT, STONE, WATER, WOOD, QUARTZ, ADOBE, MAGENTA, LIGHT_BLUE,
	YELLOW, LIME, PINK, GRAY, SILVER, CYAN, PURPLE, BLUE, BROWN, GREEN, RED, BLACK, GOLD, DIAMOND, LAPIS, EMERALD, OBSIDIAN, NETHERRACK;
	
	
	public static MapColor fromName(String name) {
		for(int i = 0; i < values().length; i++) {
			EnumMapColor mapColor = values()[i];
			if(mapColor.name().equalsIgnoreCase(name)) {
				return MapColor.COLORS[i];
			}
		}
		return MapColor.STONE;
	}
}
