package thelm.jaopca.custom.library;

import net.minecraft.block.material.Material;

public enum EnumMaterial {

	AIR(Material.AIR), ANVIL(Material.ANVIL), BARRIER(Material.BARRIER), CACTUS(Material.CACTUS),
	CAKE(Material.CAKE), CARPET(Material.CARPET), CIRCUITS(Material.CIRCUITS), CLAY(Material.CLAY),
	CLOTH(Material.CLOTH), CORAL(Material.CORAL), CRAFTED_SNOW(Material.CRAFTED_SNOW), DRAGON_EGG(Material.DRAGON_EGG),
	FIRE(Material.FIRE), GLASS(Material.GLASS), GOURD(Material.GOURD), GRASS(Material.GRASS),
	GROUND(Material.GROUND), ICE(Material.ICE), IRON(Material.IRON), LAVA(Material.LAVA),
	LEAVES(Material.LEAVES), PACKED_ICE(Material.PACKED_ICE), PISTON(Material.PISTON), PLANTS(Material.PLANTS),
	PORTAL(Material.PORTAL), REDSTONE_LIGHT(Material.REDSTONE_LIGHT), ROCK(Material.ROCK), SAND(Material.SAND),
	SNOW(Material.SNOW), SPONGE(Material.SPONGE), STRUCTURE_VOID(Material.STRUCTURE_VOID), TNT(Material.TNT),
	WATER(Material.WATER), WEB(Material.WEB), WOOD(Material.WOOD);

	Material material;

	EnumMaterial(Material material) {
		this.material = material;
	}

	public static Material fromName(String name) {
		for(EnumMaterial material : values()) {
			if(material.name().equalsIgnoreCase(name)) {
				return material.material;
			}
		}
		return Material.ROCK;
	}
}
