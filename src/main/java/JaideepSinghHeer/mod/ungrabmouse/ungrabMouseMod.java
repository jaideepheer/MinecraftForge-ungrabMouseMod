package JaideepSinghHeer.mod.ungrabmouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MouseHelper;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.Set;

@Mod(modid = ungrabMouseMod.MODID, version = ungrabMouseMod.VERSION, clientSideOnly = true, acceptedMinecraftVersions = "*",name = ungrabMouseMod.NAME, canBeDeactivated = true, guiFactory = "JaideepSinghHeer.mod.ungrabmouse.ungrabMouseMod$GUIFactory")
public class ungrabMouseMod
{
    public static final String NAME = "Ungrab Mouse Mod";
    public static final String MODID = "ungrabmouse";
    public static final String VERSION = "2.0";

    @Mod.Instance(owner = MODID)
    public static ungrabMouseMod INSTANCE = new ungrabMouseMod();

    // Ask forge to point this variable to the ModMetaData object of this mod.
    @Mod.Metadata(value = MODID)
    public static ModMetadata metadata;

    // Keybind object to register to forge.
    public static KeyBinding ungrabKeyBind;

    // Internal ungrab state variables.
    private boolean isUngrabbed = false;
    private boolean doesGameWantUngrabbed;
    private MouseHelper oldMouseHelper;
    private boolean originalFocusPauseSetting = true;

    // Settings
    private boolean blockClicksOnUngrab = true;
    private boolean clicktoregrab = true;
    private boolean regrabOnGUIchange = false;
    private static Configuration config;

    /**
     * This function sets the mod's details to be displayed in the modlist.
     * It also initialises the configuration file and object to store the mod's configs.
     * @param event: The FMLPreInitializationEvent passed to us by forge.
     */
    @EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        metadata = event.getModMetadata();
        // Set the metadata
        metadata.authorList.set(0,"Jaideep Singh Heer");
        metadata.version = VERSION;
        metadata.description = "This is a simple mod to allow players to ungrab the mouse to do other things on their PCs while the game runs.";

        // Initialise the settings.
        config = new Configuration(event.getSuggestedConfigurationFile());
        try {
            // Open the config file and load its data.
            config.load();
            SyncConfig();
            }
            catch (Exception e)
            {
                // Print error
                e.printStackTrace();
            }
    }
    /**
     * This functions initialises our keybind object to te default key and registers it in forge.
     * It also registers this class to the forge's event bus.
     * @param event
     */
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        // Register to event bus for events.
        MinecraftForge.EVENT_BUS.register(this);
        // Initialise keybind and register it.
        ungrabKeyBind = new KeyBinding("Ungrab Mouse", Keyboard.KEY_U,"key.categories.misc");
        ClientRegistry.registerKeyBinding(ungrabKeyBind);
    }

    /**
     * This function is called by forge when any mod's config is changed from the config screen.
     * We use it to update our variables and config files to the changes.
     * @param event
     */
    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        // Check if the changes were for our mod.
        if(!event.getModID().equals(MODID))return;
        // Resync config.
        SyncConfig();
    }

    /**
     * This function simple reloads the values from our config object into our private variables.
     * It also stores the latest configs to the config file.
     */
    private void SyncConfig()
    {
        // If no config object then return.
        if(config==null)return;
        // Update private variable values.
        blockClicksOnUngrab = config.getBoolean("blockClicksOnUngrab",Configuration.CATEGORY_CLIENT,true,"If set to true, this tries to block clicks when the mouse is ungrabbed.");
        clicktoregrab = config.getBoolean("clicktoregrab",Configuration.CATEGORY_CLIENT,true,"If set to true, the mouse will be regrabbed if it is clicked while in-game.");
        regrabOnGUIchange = config.getBoolean("regrabOnGUIchange",Configuration.CATEGORY_CLIENT,false,"If set to true, the mouse will be regrabbed whenever a new GUI is opened.");
        // Store to file if config changed.
        if(config.hasChanged())config.save();
    }

    /**
     * This function regrabs the mouse on GUI change.
     * It is executed by forge when a new gui is opened.
     * @param event
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void newGUIopened(GuiOpenEvent event)
    {
        if(regrabOnGUIchange)regrabMouse();
    }

    /**
     * This functions performs all steps required to ungrab the mouse.
     */
    void ungrabMouse()
    {
        Minecraft m = Minecraft.getMinecraft();
        // Return if not in-game or already ungrabbed.
        if(!m.inGameHasFocus || isUngrabbed)return;
        // Disable pause on lost focus while ungrabbed.
        originalFocusPauseSetting = m.gameSettings.pauseOnLostFocus;
        m.gameSettings.pauseOnLostFocus = false;
        if(oldMouseHelper==null) oldMouseHelper = m.mouseHelper;
        // Store the mouse grab state set by the game.
        doesGameWantUngrabbed = !Mouse.isGrabbed();
        oldMouseHelper.ungrabMouseCursor();
        m.inGameHasFocus = true;
        m.mouseHelper = new MouseHelper(){
           @Override
           public void mouseXYChange(){}
           // These functions update the mouse grab state required by the game.
           @Override
           public void grabMouseCursor(){doesGameWantUngrabbed=false;}
           @Override
           public void ungrabMouseCursor(){doesGameWantUngrabbed=true;}
        };
        isUngrabbed = true;
    }

    /**
     * This function performs all the steps required to regrab the mouse.
     */
    void regrabMouse()
    {
        // Return if mouse already flagged as ungrabbed.
        if(!isUngrabbed)return;
        Minecraft m = Minecraft.getMinecraft();
        // Restore pause on lost focus setting.
        m.gameSettings.pauseOnLostFocus = originalFocusPauseSetting;
        // Restore mouse input.
        m.mouseHelper = oldMouseHelper;
        // Regrab the mouse if the game requires a grabbed mouse.
        if(!doesGameWantUngrabbed)m.mouseHelper.grabMouseCursor();
        oldMouseHelper = null;
        isUngrabbed = false;
    }

    /**
     * This function looks for when the ungrab key is pressed.
     * It is executed by forge when any key is pressed in-game.
     * @param event
     */
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event)
    {
        // Check if it is our key that is pressed.
        if(ungrabKeyBind.isPressed())
        {
            // Toggle the mouse grab state.
            if(!isUngrabbed)ungrabMouse();
            else regrabMouse();
        }
    }

    /**
     * This function monitors the mouse input to update our grab state.
     * It can regrab on click if that setting is enabled.
     * It also cancels the mouse input event is the blockClicksOnUngrab setting is enabled.
     * @param event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouseInput(InputEvent.MouseInputEvent event)
    {
        // Check if it was a button click and not a move or scroll. Also check if clicktoregrab setting is enabled.
        if(Mouse.getEventButtonState() && clicktoregrab)
        {
            regrabMouse();
            return;
        }
        // Try to cancell mouse events if blockClicksOnUngrab is enabled and conditions meet.
        if(isUngrabbed && blockClicksOnUngrab && event.isCancelable())event.setCanceled(true);
    }

    // =============================
    //          GUI FACTORY
    //==============================
    /**
     * This class is used by forge to get a GuiScreen object for displaying in the config screen of our mod.
     */
    public static class GUIFactory implements IModGuiFactory
    {
        @Override
        public void initialize(Minecraft minecraftInstance) {

        }
        @Override
        public boolean hasConfigGui() {
            return true;
        }
        @Override
        public GuiScreen createConfigGui(GuiScreen parentScreen) {
            // Create a config gui screen using our configuration object and return it to forge.
            return new GuiConfig(parentScreen,new ConfigElement(config.getCategory(Configuration.CATEGORY_CLIENT)).getChildElements(),MODID,false,false,"Ungrab Mouse Mod Configuration Screen");
        }
        @Override
        public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
            return null;
        }
    }
}

