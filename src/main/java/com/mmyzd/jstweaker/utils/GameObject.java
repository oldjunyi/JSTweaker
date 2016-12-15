package com.mmyzd.jstweaker.utils;

import java.util.ArrayList;

import com.mmyzd.jstweaker.JSTLogger;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;

public class GameObject {

	private String prefix = "", name = "";
	private int size = 0, meta = 0;
	private NBTTagCompound tag = null;
	
	public GameObject(String str) {
		int n = str.length();
		ArrayList<String> tokens = new ArrayList<String>();
		for (int i = 0; i < n; i++) {
			char c = str.charAt(i);
			if (c == '{') {
				int j = i, count = 1, quote = 0;
				while (++j < n && count != 0) {
					c = str.charAt(j);
					if (c == '"') quote ^= 1;
					if (c == '{' && quote == 0) count++;
					if (c == '}' && quote == 0) count--;
				}
				try {
					tag = JsonToNBT.getTagFromJson(str.substring(i,  j));
				} catch (Exception e) {
					JSTLogger.get().error(e);
				}
				i = j - 1;
			} else if ("*:".indexOf(c) != -1) {
				tokens.add(String.valueOf(c));
			} else if (Character.isWhitespace(c)) {
				continue;
			} else {
				int j = i;
				StringBuilder builder = new StringBuilder();
				builder.append(c);
				while (++j < n) {
					c = str.charAt(j);
					if (Character.isWhitespace(c) || c == ':') break;
					builder.append(c);
				}
				tokens.add(builder.toString());
				i = j - 1;
			}
		}
		n = tokens.size();
		for (int i = 0; i < n; i++) {
			str = tokens.get(i);
			if (str.matches("\\d+")) {
				if ((i > 0 && tokens.get(i - 1).equals(":")) || size > 0) {
					meta = Integer.parseInt(str);
				} else {
					size = Integer.parseInt(str);
				}
			} else if (str.equals("*") && i > 0 && tokens.get(i - 1).equals(":")) {
				meta = OreDictionary.WILDCARD_VALUE;
			} else if (str.equals("*") || str.equals(":")) {
				continue;
			} else {
				if (prefix.isEmpty()) {
					prefix = str;
				} else if (name.isEmpty()) {
					name = str;
				} else {
					name += ":" + str;
				}
			}
		}
		if (name.isEmpty()) {
			name = prefix;
			prefix = "minecraft";
		}
		if (size <= 0) size = 1;
	}
	
	public ItemStack getItemStack() {
		Item item = Item.REGISTRY.getObject(new ResourceLocation(prefix, name));
		return new ItemStack(item, size, meta, tag);
	}

}
