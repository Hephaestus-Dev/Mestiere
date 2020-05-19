package dev.hephaestus.mestiere;

import dev.hephaestus.mestiere.crafting.*;
import dev.hephaestus.mestiere.skills.Perks;
import dev.hephaestus.mestiere.skills.Skills;
import dev.hephaestus.mestiere.util.Commands;
import dev.hephaestus.mestiere.util.MestiereComponent;
import dev.hephaestus.mestiere.util.MestiereConfig;
import nerdhub.cardinal.components.api.ComponentRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.event.EntityComponentCallback;
import nerdhub.cardinal.components.api.util.EntityComponents;
import nerdhub.cardinal.components.api.util.RespawnCopyStrategy;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.recipe.RecipeType;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Mestiere implements ModInitializer {
	public static final String MOD_ID = "mestiere";
	public static final String MOD_NAME = "Mestiere";
	public static final Logger LOGGER = LogManager.getLogger();

	public static final Skills SKILLS = Skills.init();
	public static final Perks PERKS = Perks.init();
	public static final MestiereConfig CONFIG = MestiereConfig.init();

	public static final Identifier SELECT_RECIPE_ID = newID("select_recipe");

//	public static final SkillRecipe.Type SMITHING = SkillRecipe.Type.register(new SimpleSkillRecipe.Type(Skills.SMITHING), SimpleSkillRecipe.SERIALIZER);
//	public static final SkillRecipe.Type NETHERITE = SkillRecipe.Type.register(new NetheriteRecipe.Type(), NetheriteRecipe.SERIALIZER);

	public static RecipeTypes TYPES;

	public static final ComponentType<MestiereComponent> COMPONENT =
		ComponentRegistry.INSTANCE.registerIfAbsent(newID("component"), MestiereComponent.class);

	static {
		EntityComponents.setRespawnCopyStrategy(COMPONENT, RespawnCopyStrategy.ALWAYS_COPY);
	}


	@Override
	public void onInitialize() {
		CommandRegistry.INSTANCE.register(false, Commands::register);

		TYPES = RecipeTypes.init();

		EntityComponentCallback.event(ServerPlayerEntity.class).register((player, components) ->
				components.put(COMPONENT, new MestiereComponent(player)));

		ServerSidePacketRegistry.INSTANCE.register(SELECT_RECIPE_ID, ((packetContext, packetByteBuf) -> {
			int syncId = packetByteBuf.readByte();
			Identifier recipeId = packetByteBuf.readIdentifier();
			PlayerEntity player = packetContext.getPlayer();
			if (player instanceof ServerPlayerEntity)
				packetContext.getTaskQueue().execute(() -> {
					SkillCraftingController controller = SkillCraftingController.getInstance(syncId);
					if (controller.setRecipe(recipeId) == ActionResult.PASS)
						controller.fillInputSlots();
				});
		}));

		ContainerProviderRegistry.INSTANCE.registerFactory(Registry.BLOCK.getId(Blocks.SMITHING_TABLE),
			(syncId, id, player, buf) -> new SkillCraftingController(syncId, Skills.SMITHING,
					new RecipeType[] {TYPES.netherite, TYPES.tools},
					player.inventory, ScreenHandlerContext.create(player.world, buf.readBlockPos())));
	}

	public static void log(String msg) {
		log("%s", msg);
	}

	public static void log(String format, Object... args) {
		LOGGER.info(String.format("[%s] %s", MOD_NAME, String.format(format, args)));
	}

	public static void debug(String msg) {
		debug("%s", msg);
	}

	public static void debug(String format, Object... args) {
		if (FabricLoader.getInstance().isDevelopmentEnvironment())
			LOGGER.info(String.format("[%s] %s", MOD_NAME, String.format(format, args)));
	}

	public static Identifier newID(String id) {
		return new Identifier(MOD_ID, id);
	}

    public enum KEY_TYPE {
        MESSAGE,
        DESCRIPTION,
        NAME
    }
}
