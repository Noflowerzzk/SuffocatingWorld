package noflower.suffocatingworld.client;

import noflower.suffocatingworld.SuffocatingWorldConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

public class SuffocatingWorldPresetScreen extends Screen {
	private static final int BOX_WIDTH = 220;
	private static final int BOX_HEIGHT = 20;
	private static final int SUGGESTION_MAX = 8;

	private final Screen parent;
	private EditBox blockIdBox;
	private Component errorMessage;
	private Identifier parsedId;
	private List<String> suggestions = List.of();
	private int suggestionIndex = -1;

	public SuffocatingWorldPresetScreen(Screen parent) {
		super(Component.translatable("screen.suffocating-world.preset.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int centerY = this.height / 2;

		this.blockIdBox = new EditBox(
			this.font,
			centerX - (BOX_WIDTH / 2),
			centerY - 8,
			BOX_WIDTH,
			BOX_HEIGHT,
			Component.translatable("screen.suffocating-world.preset.block")
		);
		this.blockIdBox.setMaxLength(128);
		this.blockIdBox.setHint(Component.translatable("screen.suffocating-world.preset.hint"));
		this.blockIdBox.setValue(SuffocatingWorldClientConfig.loadBlockId());
		this.blockIdBox.setResponder(this::validate);
		this.addRenderableWidget(this.blockIdBox);

		int buttonY = centerY + 24;
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> applyAndClose())
			.bounds(centerX - 102, buttonY, 100, 20)
			.build());
		this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
			.bounds(centerX + 2, buttonY, 100, 20)
			.build());

		validate(this.blockIdBox.getValue());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		this.renderMenuBackground(graphics);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
		graphics.drawString(
			this.font,
			Component.translatable("screen.suffocating-world.preset.block"),
			this.width / 2 - (BOX_WIDTH / 2),
			this.height / 2 - 22,
			0xA0A0A0
		);

		super.render(graphics, mouseX, mouseY, delta);
		renderSuggestions(graphics, mouseX, mouseY);

		if (this.errorMessage != null) {
			graphics.drawString(
				this.font,
				this.errorMessage,
				this.width / 2 - (BOX_WIDTH / 2),
				this.height / 2 + 52,
				0xFF5555
			);
		}
	}

	@Override
	public void onClose() {
		if (this.minecraft != null) {
			this.minecraft.setScreen(this.parent);
		}
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (this.blockIdBox != null && this.blockIdBox.isFocused() && !this.suggestions.isEmpty()) {
			int key = event.key();
			if (key == GLFW.GLFW_KEY_TAB) {
				applySuggestion();
				return true;
			}
			if (key == GLFW.GLFW_KEY_DOWN) {
				cycleSuggestion(1);
				return true;
			}
			if (key == GLFW.GLFW_KEY_UP) {
				cycleSuggestion(-1);
				return true;
			}
			if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
				applySuggestion();
				return true;
			}
		}

		return super.keyPressed(event);
	}

	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
		if (this.blockIdBox != null && !this.suggestions.isEmpty()) {
			int listX = this.blockIdBox.getX();
			int lineHeight = this.font.lineHeight + 2;
			int listHeight = lineHeight * this.suggestions.size() + 2;
			int aboveY = this.blockIdBox.getY() - listHeight - 2;
			int listY = aboveY >= 4 ? aboveY : this.blockIdBox.getY() + BOX_HEIGHT + 2;
			double x = event.x();
			double y = event.y();
			if (x >= listX && x <= listX + BOX_WIDTH && y >= listY && y <= listY + listHeight) {
				int index = (int) ((y - listY - 1) / lineHeight);
				if (index >= 0 && index < this.suggestions.size()) {
					this.suggestionIndex = index;
					applySuggestion();
					return true;
				}
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
		return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
	}

	private void applyAndClose() {
		if (this.errorMessage != null) {
			return;
		}

		String raw = this.blockIdBox.getValue().trim();
		String normalized = this.parsedId == null ? raw : this.parsedId.toString();
		SuffocatingWorldClientConfig.saveBlockId(normalized);
		SuffocatingWorldConfig.setReplacementBlockId(normalized);
		onClose();
	}

	private void validate(String value) {
		this.errorMessage = null;
		this.parsedId = null;
		updateSuggestions(value);

		if (value == null || value.isBlank()) {
			this.errorMessage = Component.translatable("screen.suffocating-world.preset.empty");
			return;
		}

		Identifier id = value.indexOf(Identifier.NAMESPACE_SEPARATOR) >= 0
			? Identifier.tryParse(value)
			: Identifier.withDefaultNamespace(value);
		if (id == null) {
			this.errorMessage = Component.translatable("screen.suffocating-world.preset.invalid");
			return;
		}

		Optional<Holder.Reference<Block>> holder = BuiltInRegistries.BLOCK.get(id);
		if (holder.isEmpty()) {
			this.errorMessage = Component.translatable("screen.suffocating-world.preset.invalid");
			return;
		}

		Block block = holder.get().value();
		if (block == Blocks.AIR) {
			this.errorMessage = Component.translatable("screen.suffocating-world.preset.invalid");
			return;
		}

		this.parsedId = id;
	}

	private void updateSuggestions(String value) {
		String raw = value == null ? "" : value.trim();
		if (raw.isEmpty()) {
			this.suggestions = List.of();
			this.suggestionIndex = -1;
			return;
		}

		Set<Identifier> keys = BuiltInRegistries.BLOCK.keySet();
		SuggestionsBuilder builder = new SuggestionsBuilder(raw, 0);
		Suggestions suggestionsResult = SharedSuggestionProvider
			.suggestResource(keys, builder, raw)
			.join();

		List<String> matches = new ArrayList<>();
		for (var suggestion : suggestionsResult.getList()) {
			String idString = suggestion.getText();
			if (!isAirId(idString)) {
				matches.add(idString);
			}
		}

		if (matches.size() > SUGGESTION_MAX) {
			matches = matches.subList(0, SUGGESTION_MAX);
		}
		this.suggestions = matches;
		this.suggestionIndex = matches.isEmpty() ? -1 : 0;
	}

	private void renderSuggestions(GuiGraphics graphics, int mouseX, int mouseY) {
		if (this.suggestions.isEmpty() || this.blockIdBox == null || !this.blockIdBox.isFocused()) {
			return;
		}

		int listX = this.blockIdBox.getX();
		int lineHeight = this.font.lineHeight + 2;
		int listHeight = lineHeight * this.suggestions.size() + 2;
		int aboveY = this.blockIdBox.getY() - listHeight - 2;
		int listY = aboveY >= 4 ? aboveY : this.blockIdBox.getY() + BOX_HEIGHT + 2;
		graphics.fill(listX, listY, listX + BOX_WIDTH, listY + listHeight, 0xC0101010);

		for (int i = 0; i < this.suggestions.size(); i++) {
			int y = listY + 1 + i * lineHeight;
			if (i == this.suggestionIndex) {
				graphics.fill(listX + 1, y, listX + BOX_WIDTH - 1, y + lineHeight, 0x80FFFFFF);
			}
			graphics.drawString(this.font, this.suggestions.get(i), listX + 4, y + 2, 0xFFE0E0E0);
		}
	}

	private void applySuggestion() {
		if (this.suggestions.isEmpty() || this.suggestionIndex < 0) {
			return;
		}
		String value = this.suggestions.get(this.suggestionIndex);
		this.blockIdBox.setValue(value);
		this.blockIdBox.moveCursorToEnd(false);
	}

	private void cycleSuggestion(int step) {
		if (this.suggestions.isEmpty()) {
			return;
		}
		int size = this.suggestions.size();
		int next = this.suggestionIndex + step;
		if (next < 0) {
			next = size - 1;
		} else if (next >= size) {
			next = 0;
		}
		this.suggestionIndex = next;
	}

	private static boolean isAirId(String idString) {
		return "minecraft:air".equals(idString)
			|| "minecraft:cave_air".equals(idString)
			|| "minecraft:void_air".equals(idString);
	}
}
