package noflower.suffocatingworld.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;

public class SuffocatingWorldPresetEditor implements PresetEditor {
	@Override
	public Screen createEditScreen(CreateWorldScreen parent, WorldCreationContext context) {
		return new SuffocatingWorldPresetScreen(parent);
	}
}
