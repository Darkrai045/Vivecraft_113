package org.vivecraft.gui.framework;

import java.awt.Rectangle;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.optifine.gui.TooltipProvider;

public class TooltipProviderVROptions implements TooltipProvider
{
    public Rectangle getTooltipBounds(GuiScreen guiScreen, int x, int y)
    {
        int i = guiScreen.width / 2 - 150;
        int j = guiScreen.height / 6 - 7;

        if (y <= j + 98)
        {
            j += 105;
        }

        int k = i + 150 + 150;
        int l = j + 84 + 10;
        return new Rectangle(i, j, k - i, l - j);
    }

    public boolean isRenderBorder()
    {
        return false;
    }

    public String[] getTooltipLines(GuiButton btn, int width)
    {
    	
    	if(btn instanceof GuiVROptionButton) {
    		return ((GuiVROptionButton)btn).getToolTip();
    	}	
    	else if(btn instanceof GuiVROptionSlider) {
    		return ((GuiVROptionSlider)btn).getToolTip();
    	}	
        else
        {
        	return null;
        }
    }

    public static String[] getTooltipLines(String key)
    {
    	return null;
    }
}
