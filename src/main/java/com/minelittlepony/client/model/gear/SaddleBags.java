package com.minelittlepony.client.model.gear;

import com.minelittlepony.client.util.render.Part;
import com.minelittlepony.model.BodyPart;
import com.minelittlepony.model.IModel;
import com.minelittlepony.model.IPegasus;
import com.minelittlepony.pony.meta.Race;
import com.minelittlepony.pony.meta.Wearable;

import java.util.UUID;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class SaddleBags extends AbstractGear {

    public static final Identifier TEXTURE = new Identifier("minelittlepony", "textures/models/saddlebags.png");

    private ModelPart leftBag;
    private ModelPart rightBag;

    private ModelPart strap;

    private boolean hangLow = false;

    float dropAmount = 0;


    private IModel model;

    @Deprecated
    public void init(float yOffset, float stretch) {
        leftBag = new Part(this, 56, 19);
        rightBag = new Part(this, 56, 19);
        strap = new Part(this, 56, 19);

        float y = -0.5F;
        int x = 4;
        int z = -1;

        ((Part)strap).offset(-x, y + 0.2F, z + 3).around(0, 4, 4)
        .tex(56, 31).top(0, 0, 0, 8, 1, stretch)
                    .top(0, 0, 1, 8, 1, stretch)
                  .south(0, 0, 2, 8, 1, stretch)
                  .north(0, 0, 0, 8, 1, stretch)
                .child(0).offset(0, -3, -0.305F).tex(56, 31)
                   .west( 4.0002F,  0, 0, 1, 3, stretch)  // 0.0001 is there
                   .west( 4.0002F, -1, 0, 1, 3, stretch)  // otherwise straps
                   .west(-4.0002F,  0, 0, 1, 3, stretch)  // clip into the body
                   .west(-4.0002F, -1, 0, 1, 3, stretch)
                .pitch = ROTATE_270;

        ((Part)leftBag).offset(x, y, z).around(0, 4, 4)
                .tex(56, 25).south(0, 0, 0, 3, 6, stretch)
                .tex(59, 25).south(0, 0, 8, 3, 6, stretch)
                .tex(56, 19) .west(3, 0, 0, 6, 8, stretch)
                             .west(0, 0, 0, 6, 8, stretch)
                .child(0).offset(z, y, -x).tex(56, 16)
                                     .top(0, 0, -3, 8, 3, stretch)
              .tex(56, 22).flipZ().bottom(0, 6, -3, 8, 3, stretch)
                         .yaw = ROTATE_270;

        x += 3;

        ((Part)rightBag).offset(-x, y, z).around(0, 4, 4).flipX()
                .tex(56, 25).south(0, 0, 0, 3, 6, stretch)
                .tex(59, 25).south(0, 0, 8, 3, 6, stretch)
                .tex(56, 19).west(3, 0, 0, 6, 8, stretch)
                            .west(0, 0, 0, 6, 8, stretch)
                   .child(0).offset(z, y, x).tex(56, 16)
                            .flipZ().top(0, 0, -3, 8, 3, stretch)
             .tex(56, 22).flipZ().bottom(0, 6, -3, 8, 3, stretch)
                 .yaw = ROTATE_270;
    }

    @Override
    public void setLivingAnimations(IModel model, Entity entity) {
        this.model = model;

        hangLow = false;

        if (model instanceof IPegasus) {
            hangLow = model.canFly() && ((IPegasus)model).wingsAreOpen();
        }
    }

    @Override
    public void setRotationAndAngles(boolean rainboom, UUID interpolatorId, float move, float swing, float bodySwing, float ticks) {
        float pi = PI * (float) Math.pow(swing, 16);

        float mve = move * 0.6662f;
        float srt = swing / 10;

        bodySwing = MathHelper.cos(mve + pi) * srt;

        leftBag.pitch = bodySwing;
        rightBag.pitch = bodySwing;

        if (model instanceof IPegasus && model.isFlying()) {
            bodySwing = ((IPegasus)model).getWingRotationFactor(ticks) - ROTATE_270;
            bodySwing /= 10;
        }

        leftBag.roll = bodySwing;
        rightBag.roll = -bodySwing;

        dropAmount = hangLow ? 0.15F : 0;
    }

    public void sethangingLow(boolean veryLow) {
        hangLow = veryLow;
    }

    @Override
    public void renderPart(MatrixStack stack, VertexConsumer renderContext, int overlayUv, int lightUv, float red, float green, float blue, float alpha, UUID interpolatorId) {
        dropAmount = model.getMetadata().getInterpolator(interpolatorId).interpolate("dropAmount", dropAmount, 3);

        stack.push();
        stack.translate(0, dropAmount, 0);

        leftBag.render(stack, renderContext, overlayUv, lightUv, red, green, blue, alpha);
        rightBag.render(stack, renderContext, overlayUv, lightUv, red, green, blue, alpha);

        stack.pop();
        strap.render(stack, renderContext, overlayUv, lightUv, red, green, blue, alpha);
    }

    @Override
    public boolean canRender(IModel model, Entity entity) {
        return model.isWearing(Wearable.SADDLE_BAGS);
    }

    @Override
    public BodyPart getGearLocation() {
        return BodyPart.BODY;
    }

    @Override
    public <T extends Entity> Identifier getTexture(T entity, IRenderContext<T, ?> context) {
        if (context.getEntityModel() != null && context.getEntityModel().getMetadata().getRace().isEquivalentTo(Race.CHANGELING)) {
            return TEXTURE;
        }
        return context.getDefaultTexture(entity, this);
    }
}
