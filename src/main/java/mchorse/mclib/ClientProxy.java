package mchorse.mclib;

import mchorse.mclib.client.KeyboardHandler;
import mchorse.mclib.client.MouseRenderer;
import mchorse.mclib.client.gui.mclib.GuiDashboard;
import mchorse.mclib.client.gui.utils.keys.LangKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy
{
	public static GuiDashboard dashboard;

	public static GuiDashboard getDashboard()
	{
		if (dashboard == null)
		{
			dashboard = new GuiDashboard(Minecraft.getMinecraft());
		}

		return dashboard;
	}

	@Override
	public void preInit(FMLPreInitializationEvent event)
	{
		super.preInit(event);

		MinecraftForge.EVENT_BUS.register(new KeyboardHandler());
		MinecraftForge.EVENT_BUS.register(new MouseRenderer());
	}

	@Override
	public void init(FMLInitializationEvent event)
	{
		super.init(event);

		Minecraft mc = Minecraft.getMinecraft();

		/* OMG, thank you very much Forge! */
		if (!mc.getFramebuffer().isStencilEnabled())
		{
			mc.getFramebuffer().enableStencil();
		}

		((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener((manager) -> LangKey.lastTime = System.currentTimeMillis());
	}
}