package thelm.jaopca.custom.module;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraftforge.common.config.Configuration;
import thelm.jaopca.api.IItemRequest;
import thelm.jaopca.api.JAOPCAApi;
import thelm.jaopca.api.ModuleBase;
import thelm.jaopca.custom.json.ItemRequestDeserializer;
import thelm.jaopca.custom.json.OreFunctionDeserializers;

public class ModuleCustom extends ModuleBase {

	public static final ArrayList<IItemRequest> REQUESTS = Lists.<IItemRequest>newArrayList();
	private static final Gson GSON = new GsonBuilder().
			registerTypeAdapter(IItemRequest.class, ItemRequestDeserializer.INSTANCE).
			registerTypeAdapter(OreFunctionDeserializers.BOOLEAN_TYPE, OreFunctionDeserializers.OrePredicateDeserializer.INSTANCE).
			registerTypeAdapter(OreFunctionDeserializers.DOUBLE_TYPE, OreFunctionDeserializers.OreToDoubleFunctionDeserializer.INSTANCE).
			registerTypeAdapter(OreFunctionDeserializers.FLOAT_TYPE, OreFunctionDeserializers.OreToFloatFunctionDeserializer.INSTANCE).
			registerTypeAdapter(OreFunctionDeserializers.LONG_TYPE, OreFunctionDeserializers.OreToLongFunctionDeserializer.INSTANCE).
			registerTypeAdapter(OreFunctionDeserializers.INT_TYPE, OreFunctionDeserializers.OreToIntFunctionDeserializer.INSTANCE).
			create();
	private static File file;
	public static Configuration config;

	@Override
	public String getName() {
		return "custom";
	}

	@Override
	public void registerConfigsPre(Configuration configFile) {
		config = configFile;
		OreFunctionDeserializers.config = configFile;
		if(file != null) {
			try {
				if(file.exists()) {
					List<IItemRequest> list = GSON.<List<IItemRequest>>fromJson(new FileReader(file), new TypeToken<List<IItemRequest>>(){}.getType());
					if(list != null) {
						REQUESTS.addAll(list);
					}
				}
				else {
					FileWriter e = new FileWriter(file);
					e.close();
					List<IItemRequest> list = GSON.<List<IItemRequest>>fromJson(new FileReader(file), new TypeToken<List<IItemRequest>>(){}.getType());
					if(list != null) {
						REQUESTS.addAll(list);
					}
				}

				file = null;
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public List<? extends IItemRequest> getItemRequests() {
		return REQUESTS;
	}

	public static void init(File file) {
		JAOPCAApi.registerModule(new ModuleCustom());
		ModuleCustom.file = file;
	}
}
