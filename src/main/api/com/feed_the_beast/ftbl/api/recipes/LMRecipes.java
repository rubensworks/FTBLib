package com.feed_the_beast.ftbl.api.recipes;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.ArrayList;

public class LMRecipes
{
    public static final LMRecipes INSTANCE = new LMRecipes();

    public boolean enableOreRecipes = true;

    public static ItemStack size(ItemStack is, int s)
    {
        ItemStack is1 = is.copy();
        is1.stackSize = s;
        return is1;
    }

    public IRecipe addIRecipe(IRecipe r)
    {
        CraftingManager.getInstance().getRecipeList().add(r);
        return r;
    }

    public Object[] fixObjects(Object[] in)
    {
        for(int i = 0; i < in.length; i++)
        {
            Object o = StackArray.getFrom(in[i]);
            if(o != null)
            {
                in[i] = o;
            }
        }

        return in;
    }

    public IRecipe addRecipe(ItemStack out, Object... in)
    {
        in = fixObjects(in);
        IRecipe r;

        if(!enableOreRecipes)
        {
            r = GameRegistry.addShapedRecipe(out, in);
        }
        else
        {
            r = addIRecipe(new ShapedOreRecipe(out, in));
        }

        return r;
    }

    public IRecipe addShapelessRecipe(ItemStack out, Object... in)
    {
        in = fixObjects(in);

        if(!enableOreRecipes)
        {
            ArrayList<ItemStack> al = new ArrayList<>();

            for(Object anIn : in)
            {
                ItemStack is = StackArray.getFrom(anIn);
                if(is != null)
                {
                    al.add(is);
                }
                else
                {
                    throw new RuntimeException("Invalid shapeless recipy!");
                }
            }

            return addIRecipe(new ShapelessRecipes(out, al));
        }

        return addIRecipe(new ShapelessOreRecipe(out, in));
    }

    public void addItemBlockRecipe(ItemStack block, ItemStack item, boolean back, boolean small)
    {
        if(small)
        {
            addRecipe(block, "EE", "EE", 'E', item);

            if(back)
            {
                ItemStack out4 = item.copy();
                out4.stackSize = 4;
                addShapelessRecipe(out4, block);
            }
        }
        else
        {
            addRecipe(block, "EEE", "EEE", "EEE", 'E', item);

            if(back)
            {
                ItemStack out9 = item.copy();
                out9.stackSize = 9;
                addShapelessRecipe(out9, block);
            }
        }
    }

    public void addSmelting(ItemStack out, ItemStack in, float xp)
    {
        FurnaceRecipes.instance().addSmeltingRecipe(in, out, xp);
    }

    public void addSmelting(ItemStack out, ItemStack in)
    {
        addSmelting(out, in, 0F);
    }

    public void loadRecipes()
    {
    }
}