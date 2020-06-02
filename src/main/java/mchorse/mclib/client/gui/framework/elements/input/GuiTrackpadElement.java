package mchorse.mclib.client.gui.framework.elements.input;

import mchorse.mclib.McLib;
import mchorse.mclib.client.gui.framework.elements.GuiElement;
import mchorse.mclib.client.gui.framework.elements.IFocusedGuiElement;
import mchorse.mclib.client.gui.framework.elements.utils.GuiContext;
import mchorse.mclib.client.gui.framework.elements.utils.GuiDraw;
import mchorse.mclib.client.gui.utils.Area;
import mchorse.mclib.client.gui.utils.Icons;
import mchorse.mclib.client.gui.utils.keys.IKey;
import mchorse.mclib.config.values.ValueDouble;
import mchorse.mclib.config.values.ValueFloat;
import mchorse.mclib.config.values.ValueInt;
import mchorse.mclib.utils.ColorUtils;
import mchorse.mclib.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.function.Consumer;

public class GuiTrackpadElement extends GuiElement implements IFocusedGuiElement
{
    public static final DecimalFormat FORMAT;

    public Consumer<Double> callback;
    public GuiTextField text;

    public double value;

    /* Trackpad options */
    public double strong = 1F;
    public double normal = 0.25F;
    public double weak = 0.05F;
    public double increment = 1;
    public double min = Float.NEGATIVE_INFINITY;
    public double max = Float.POSITIVE_INFINITY;
    public boolean integer;

    /* Value dragging fields */
    private boolean dragging;
    private int lastX;
    private int lastY;
    private double lastValue;

    private long time;
    private Area plusOne = new Area();
    private Area minusOne = new Area();

    static
    {
        FORMAT = new DecimalFormat("#.###");
        FORMAT.setRoundingMode(RoundingMode.CEILING);
    }

    public GuiTrackpadElement(Minecraft mc, ValueInt value)
    {
        this(mc, value, null);
    }

    public GuiTrackpadElement(Minecraft mc, ValueInt value, Consumer<Double> callback)
    {
        this(mc, callback == null ? (v) -> value.set(v.intValue()) : (v) ->
        {
            value.set(v.intValue());
            callback.accept(v);
        });
        this.limit(value.min, value.max, true);
        this.setValue(value.get());
        this.tooltip(IKey.lang(value.getTooltipKey()));
    }

    public GuiTrackpadElement(Minecraft mc, ValueFloat value)
    {
        this(mc, value, null);
    }

    public GuiTrackpadElement(Minecraft mc, ValueFloat value, Consumer<Double> callback)
    {
        this(mc, callback == null ? (v) -> value.set(v.floatValue()) : (v) ->
        {
            value.set(v.floatValue());
            callback.accept(v);
        });
        this.limit(value.min, value.max);
        this.setValue(value.get());
        this.tooltip(IKey.lang(value.getTooltipKey()));
    }

    public GuiTrackpadElement(Minecraft mc, ValueDouble value)
    {
        this(mc, value, null);
    }

    public GuiTrackpadElement(Minecraft mc, ValueDouble value, Consumer<Double> callback)
    {
        this(mc, callback == null ? value::set : (v) ->
        {
            value.set(v);
            callback.accept(v);
        });
        this.limit((float) value.min, (float) value.max);
        this.setValue((float) value.get());
        this.tooltip(IKey.lang(value.getTooltipKey()));
    }

    public GuiTrackpadElement(Minecraft mc, Consumer<Double> callback)
    {
        super(mc);

        this.callback = callback;

        this.text = new GuiTextField(0, font, 0, 0, 0, 0);
        this.text.setEnableBackgroundDrawing(false);
        this.setValue(0);

        this.flex().h(20);
    }

    public GuiTrackpadElement max(double max)
    {
        this.max = max;

        return this;
    }

    public GuiTrackpadElement limit(double min)
    {
        this.min = min;

        return this;
    }

    public GuiTrackpadElement limit(double min, double max)
    {
        this.min = min;
        this.max = max;

        return this;
    }

    public GuiTrackpadElement limit(double min, double max, boolean integer)
    {
        this.integer = integer;

        return this.limit(min, max);
    }

    public GuiTrackpadElement integer()
    {
        this.integer = true;

        return this;
    }

    public GuiTrackpadElement increment(double increment)
    {
        this.increment = increment;

        return this;
    }

    public GuiTrackpadElement values(double normal)
    {
        this.normal = normal;
        this.weak = normal / 5F;
        this.strong = normal * 5F;

        return this;
    }

    public GuiTrackpadElement values(double normal, double weak, double strong)
    {
        this.normal = normal;
        this.weak = weak;
        this.strong = strong;

        return this;
    }

    /* Values presets */

    public GuiTrackpadElement degrees()
    {
        return this.increment(15D).values(1D, 0.1D, 5D  );
    }

    public GuiTrackpadElement block()
    {
        return this.increment(1 / 16D).values(1 / 32D, 1 / 128D, 1 / 2D);
    }

    /**
     * Whether this trackpad is dragging
     */
    public boolean isDragging()
    {
        return this.dragging;
    }

    public boolean isDraggingTime()
    {
        return this.isDragging() && System.currentTimeMillis() - this.time > 150;
    }

    /**
     * Set the value of the field. The input value would be rounded up to 3
     * decimal places.
     */
    public void setValue(double value)
    {
        value = Math.round(value * 1000F) / 1000F;
        value = MathUtils.clamp(value, this.min, this.max);

        if (this.integer)
        {
            value = (int) value;
        }

        this.value = value;
        this.text.setText(this.integer ? String.valueOf((int) value) : FORMAT.format(value));
        this.text.setCursorPositionZero();
    }

    /**
     * Set value of this field and also notify the trackpad listener so it
     * could detect the value change.
     */
    public void setValueAndNotify(double value)
    {
        this.setValue(value);

        if (this.callback != null)
        {
            this.callback.accept(value);
        }
    }

    /**
     * Update the bounding box of this GUI field
     */
    @Override
    public void resize()
    {
        super.resize();

        this.text.setCursorPositionZero();
        this.plusOne.copy(this.area);
        this.minusOne.copy(this.area);
        this.plusOne.w = this.minusOne.w = 20;
        this.plusOne.x = this.area.ex() - 20;
    }

    /**
     * Delegates mouse click to text field and initiate value dragging if the
     * cursor inside of trackpad's bounding box.
     */
    @Override
    public boolean mouseClicked(GuiContext context)
    {
        if (super.mouseClicked(context))
        {
            return true;
        }

        if (context.mouseButton == 0)
        {
            boolean wasFocused = this.text.isFocused();

            this.text.mouseClicked(context.mouseX, context.mouseY, context.mouseButton);

            if (wasFocused != this.text.isFocused())
            {
                context.focus(wasFocused ? null : this);
            }

            if (this.area.isInside(context))
            {
                if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
                {
                    this.setValueAndNotify(Math.round(this.value));

                    return true;
                }

                if (!this.text.isFocused() && !(this.plusOne.isInside(context) || this.minusOne.isInside(context)))
                {
                    context.focus(this);
                }

                this.dragging = true;
                this.lastX = context.mouseX;
                this.lastY = context.mouseY;
                this.lastValue = this.value;
                this.time = System.currentTimeMillis();
            }
        }

        return this.area.isInside(context);
    }

    /**
     * Reset value dragging
     */
    @Override
    public void mouseReleased(GuiContext context)
    {
        if (this.dragging && !this.isDraggingTime() && context.mouseButton == 0)
        {
            if (this.plusOne.isInside(context))
            {
                this.setValueAndNotify(this.value + this.increment);
            }
            else if (this.minusOne.isInside(context))
            {
                this.setValueAndNotify(this.value - this.increment);
            }
        }

        this.dragging = false;

        super.mouseReleased(context);
    }

    @Override
    public boolean keyTyped(GuiContext context)
    {
        if (super.keyTyped(context))
        {
            return true;
        }

        if (this.isFocused())
        {
            if (context.keyCode == Keyboard.KEY_TAB)
            {
                context.focus(this, -1, GuiScreen.isShiftKeyDown() ? -1 : 1);

                return true;
            }
            else if (context.keyCode == Keyboard.KEY_ESCAPE)
            {
                context.unfocus();

                return true;
            }
        }

        String old = this.text.getText();
        boolean result = this.text.textboxKeyTyped(context.typedChar, context.keyCode);
        String text = this.text.getText();

        if (this.text.isFocused() && !text.equals(old))
        {
            try
            {
                this.value = text.isEmpty() ? 0 : Float.parseFloat(text);

                if (this.callback != null)
                {
                    this.callback.accept(value);
                }
            }
            catch (Exception e)
            {}
        }

        return result;
    }

    @Override
    public boolean isFocused()
    {
        return this.text.isFocused();
    }

    @Override
    public void focus(GuiContext context)
    {
        this.text.setFocused(true);
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void unfocus(GuiContext context)
    {
        this.text.setFocused(false);
        Keyboard.enableRepeatEvents(false);
    }

    /**
     * Draw the trackpad
     *
     * This method will not only draw the text box, background and title label,
     * but also dragging the numerical value based on the mouse input.
     */
    @Override
    public void draw(GuiContext context)
    {
        int x = this.area.x;
        int y = this.area.y;
        int w = this.area.w;
        int h = this.area.h;
        int padding = McLib.enableBorders.get() ? 1 : 0;

        this.area.draw(0xff000000);

        boolean dragging = this.isDraggingTime();
        boolean plus = !dragging && this.plusOne.isInside(context);
        boolean minus = !dragging && this.minusOne.isInside(context);

        if (dragging)
        {
            /* Draw filling background */
            int color = McLib.primaryColor.get();
            int fx = MathUtils.clamp(context.mouseX, this.area.x + padding, this.area.ex() - padding);

            Gui.drawRect(Math.min(fx, this.lastX), this.area.y + padding, Math.max(fx, this.lastX), this.area.ey() - padding, 0xff000000 + color);
        }
        else if (plus)
        {
            this.plusOne.draw(0x22ffffff, padding);
        }
        else if (minus)
        {
            this.minusOne.draw(0x22ffffff, padding);
        }

        GlStateManager.enableBlend();
        ColorUtils.bindColor(minus ? 0xffffffff : 0x80ffffff);
        Icons.MOVE_LEFT.render(x + 5, y + (h - 16) / 2);
        ColorUtils.bindColor(plus ? 0xffffffff : 0x80ffffff);
        Icons.MOVE_RIGHT.render(x + w - 13, y + (h - 16) / 2);
        GlStateManager.disableBlend();

        int width = MathUtils.clamp(this.font.getStringWidth(this.text.getText()), 0, w - 16);

        this.text.x = this.area.mx(width);
        this.text.y = this.area.my() - 4;
        this.text.width = width + 6;
        this.text.height = 9;
        this.text.drawTextBox();

        if (dragging)
        {
            if (this.isFocused())
            {
                context.unfocus();
            }

            int dx = context.mouseX - this.lastX;
            int dy = context.mouseY - this.lastY;

            if (dx != 0 || dy != 0)
            {
                double value = this.normal;

                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
                {
                    value = this.strong;
                }
                else if (Keyboard.isKeyDown(Keyboard.KEY_LMENU))
                {
                    value = this.weak;
                }

                double diff = ((int) Math.sqrt(dx * dx + dy * dy) - 3) * value;
                double newValue = this.lastValue + (dx < 0 ? -diff : diff);

                newValue = diff < 0 ? this.lastValue : Math.round(newValue * 1000F) / 1000F;

                if (this.value != newValue)
                {
                    this.setValueAndNotify(MathUtils.clamp(newValue, this.min, this.max));
                }
            }

            /* Draw active element */
            Gui.drawRect(this.lastX - 4, this.lastY - 4, this.lastX - 3, this.lastY + 4, 0xffffffff);
            Gui.drawRect(this.lastX + 3, this.lastY - 4, this.lastX + 4, this.lastY + 4, 0xffffffff);
            Gui.drawRect(this.lastX - 3, this.lastY - 4, this.lastX + 3, this.lastY - 3, 0xffffffff);
            Gui.drawRect(this.lastX - 3, this.lastY + 3, this.lastX + 3, this.lastY + 4, 0xffffffff);
        }

        GuiDraw.drawLockedArea(this);

        super.draw(context);
    }
}