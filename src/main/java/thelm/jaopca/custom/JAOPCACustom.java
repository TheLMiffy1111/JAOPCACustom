package thelm.jaopca.custom;

import java.io.File;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import thelm.jaopca.custom.module.ModuleCustom;

@Mod(
		modid = JAOPCACustom.MOD_ID,
		name = "JAOPCASingularities",
		version = JAOPCACustom.VERSION,
		dependencies = "required-before:jaopca@[1.10.2-1.0.24,)"
		)
public class JAOPCACustom {
	public static final String MOD_ID = "jaopcacustom";
	public static final String VERSION = "1.10.2-1.0.0";
	@Instance(JAOPCACustom.MOD_ID)
	public static JAOPCACustom core;
	public static ModMetadata metadata;

	@EventHandler
	public void firstMovement(FMLPreInitializationEvent event) {
		metadata = event.getModMetadata();
		metadata.autogenerated = false;
		metadata.modId = MOD_ID;
		metadata.version = VERSION;
		metadata.name = "Just A Ore Processing Compatibility Attempt: Customized";
		metadata.credits = "Idea taken from AOBD's custom material system";
		metadata.authorList.add("TheLMiffy1111");
		metadata.description = "A mod that aims to add user-configured materials for ores.";
		
		ModuleCustom.init(new File(event.getModConfigurationDirectory(), "JAOPCACustom.json"));
	}
}