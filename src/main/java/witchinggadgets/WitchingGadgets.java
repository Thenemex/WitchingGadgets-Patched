package witchinggadgets;

import nemexlib.api.util.Logger;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.common.MinecraftForge;

import witchinggadgets.common.CommonProxy;
import witchinggadgets.common.WGConfig;
import witchinggadgets.common.WGContent;
import witchinggadgets.common.WGModCompat;
import witchinggadgets.common.util.WGCreativeTab;
import witchinggadgets.common.util.handler.EventHandler;
import witchinggadgets.common.util.handler.PlayerTickHandler;
import witchinggadgets.common.util.handler.WGWandManager;
import witchinggadgets.common.util.network.message.MessageClientNotifier;
import witchinggadgets.common.util.network.message.MessagePlaySound;
import witchinggadgets.common.util.network.message.MessagePrimordialGlove;
import witchinggadgets.common.util.network.message.MessageTileUpdate;
import witchinggadgets.common.world.VillageComponentPhotoshop;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.VillagerRegistry;
import cpw.mods.fml.relauncher.Side;

import static cpw.mods.fml.common.registry.GameRegistry.Type.BLOCK;
import static cpw.mods.fml.common.registry.GameRegistry.Type.ITEM;
import static witchinggadgets.WitchingGadgets.dependencies;
import static witchinggadgets.common.WGContent.*;

@Mod(modid = WitchingGadgets.MODID, name = WitchingGadgets.MODNAME, version = WitchingGadgets.VERSION, dependencies = dependencies)
public class WitchingGadgets
{
	public static final String MODID = "WitchingGadgets";
	public static final String MODNAME = "Witching Gadgets : Patched";
	public static final String VERSION = "1.2.1";

	public PlayerTickHandler playerTickHandler;

	public final WGWandManager wgWandManager = new WGWandManager();

	public static final CreativeTabs tabWG = new WGCreativeTab(CreativeTabs.getNextID(), "witchinggadgets");
	public static final Logger logger = new Logger("WG");
	public EventHandler eventHandler;

	@Instance("WitchingGadgets")
	public static WitchingGadgets instance = new WitchingGadgets();

	@SidedProxy(clientSide="witchinggadgets.client.ClientProxy", serverSide="witchinggadgets.common.CommonProxy")
	public static CommonProxy proxy;

	public static SimpleNetworkWrapper packetHandler;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		logger.info("Setting up 'WitchingGadgets'");

		WGConfig.loadConfig(event);
		WGContent.preInit();

		packetHandler = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

		eventHandler = new EventHandler();
		MinecraftForge.EVENT_BUS.register(eventHandler);
		playerTickHandler = new PlayerTickHandler();
		FMLCommonHandler.instance().bus().register(eventHandler);
		FMLCommonHandler.instance().bus().register(playerTickHandler);


		VillagerRegistry.instance().registerVillageCreationHandler(new VillageComponentPhotoshop.VillageManager());
		try
		{
			MapGenStructureIO.func_143031_a(VillageComponentPhotoshop.class, "WGVillagePhotoWorkshop");
		}
		catch (Exception e)
		{
			logger.error("Photographer's Workshop not added to Villages");
		}
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.registerRenders();
		//		WGPacketPipeline.INSTANCE.initialise();

		WGContent.init();

		proxy.registerHandlers();
		NetworkRegistry.INSTANCE.registerGuiHandler(instance, proxy);

		packetHandler.registerMessage(MessageClientNotifier.HandlerClient.class, MessageClientNotifier.class, 0, Side.CLIENT);
		packetHandler.registerMessage(MessagePlaySound.HandlerClient.class, MessagePlaySound.class, 1, Side.CLIENT);
		packetHandler.registerMessage(MessagePrimordialGlove.HandlerServer.class, MessagePrimordialGlove.class, 2, Side.SERVER);
		packetHandler.registerMessage(MessageTileUpdate.HandlerClient.class, MessageTileUpdate.class, 3, Side.CLIENT);
		packetHandler.registerMessage(MessageTileUpdate.HandlerServer.class, MessageTileUpdate.class, 4, Side.SERVER);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		WGModCompat.init();
		WGContent.postInit();
		//		WGPacketPipeline.INSTANCE.postInitialise();
	}

	@Mod.EventHandler
	public void missingMappings(FMLMissingMappingsEvent event) {
		boolean related = false;
		int cptBlock = 0, cptItem = 0, cptItemBlock = 0;
		String prefix = MODID.concat(":");
		for (MissingMapping mapping : event.get())
			if (mapping.name.startsWith(prefix))
				try {
					related = true;
					String s = mapping.name.substring(prefix.length());
					// Blocks
					if (mapping.type.equals(BLOCK))
						for (Block block : blockList)
                            if (s.substring(3).equals(block.getUnlocalizedName().substring(5))) {
								mapping.remap(block);
								cptBlock++;
							}
					// Items
					if (mapping.type.equals(ITEM))
						for (Item item : itemList)
							if (s.substring(8).equals(item.getUnlocalizedName().substring(5))) {
								mapping.remap(item);
								cptItem++;
							}
					// ItemBlocks
					if (mapping.type.equals(ITEM))
						for (ItemBlock itemBlock : itemBlockList) {
							logger.info(s, "MATCHES?", itemBlock.getUnlocalizedName());
							if (s.substring(3).equals(itemBlock.getUnlocalizedName().substring(5))) {
								mapping.remap(itemBlock);
								cptItemBlock++;
							}
						}
				} catch (Exception ignored) {}
		if (related) logger.info("Successfully remapped", cptBlock, "Blocks,", cptItemBlock, "ItemBlocks and", cptItem, "Items !");
	}

	public static final String dependencies =
			"required-after:Thaumcraft;" +
			"required-after:TravellersGear@[1.16.4,);" +
			"required-after:NemexLib@[1.11.4,);" +
			"after:TwilightForest;" +
			"after:Mystcraft;" +
			"after:TConstruct;" +
			"after:MagicBees;" +
			"after:ForgeMultipart";
}