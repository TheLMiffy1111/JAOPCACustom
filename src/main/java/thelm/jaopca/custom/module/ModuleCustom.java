package thelm.jaopca.custom.module;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import thelm.jaopca.api.IItemRequest;
import thelm.jaopca.api.JAOPCAApi;
import thelm.jaopca.api.ModuleBase;
import thelm.jaopca.custom.json.ItemRequestDeserializer;

public class ModuleCustom extends ModuleBase {

	public static final ArrayList<IItemRequest> REQUESTS = Lists.<IItemRequest>newArrayList();
	private static final Gson GSON = new GsonBuilder().registerTypeAdapter(IItemRequest.class, new ItemRequestDeserializer()).create();
	private static final ParameterizedType TYPE = new ParameterizedType() {
		public Type[] getActualTypeArguments() {
			return new Type[] {IItemRequest.class};
		}
		public Type getRawType()  {
			return List.class;
		}
		public Type getOwnerType() {
			return null;
		}
	};

	@Override
	public String getName() {
		return "custom";
	}

	@Override
	public List<? extends IItemRequest> getItemRequests() {
		return REQUESTS;
	}

	public static void init(File file) {
		JAOPCAApi.registerModule(new ModuleCustom());
		try {
			if(file.exists()) {
				REQUESTS.addAll(GSON.<List<IItemRequest>>fromJson(new FileReader(file), TYPE));
			}
			else {
				FileWriter e = new FileWriter(file);
				e.close();
				REQUESTS.addAll(GSON.<List<IItemRequest>>fromJson(new FileReader(file), TYPE));
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
