package net.conczin.equipment.interactions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.iterator.BlockIterator;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionSyncData;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.asset.type.fluid.FluidTicker;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.InteractionConfiguration;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is a copy of RefillContainerInteraction but instead of "refilling" it "fills durability"
 */
public class FillContainerInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<FillContainerInteraction> CODEC = BuilderCodec.builder(
                    FillContainerInteraction.class, FillContainerInteraction::new, SimpleInstantInteraction.CODEC
            )
            .documentation("Refills a container item that is currently held.")
            .appendInherited(
                    new KeyedCodec<>("States", new MapCodec<>(FillContainerInteraction.RefillState.CODEC, HashMap::new)),
                    (interaction, v) -> interaction.refillStateMap = v,
                    interaction -> interaction.refillStateMap,
                    (o, p) -> o.refillStateMap = p.refillStateMap
            )
            .addValidator(Validators.nonNull())
            .add()
            .afterDecode(refillContainerInteraction -> {
                refillContainerInteraction.allowedFluidIds = null;
                refillContainerInteraction.fluidToState = null;
            })
            .build();
    protected Map<String, FillContainerInteraction.RefillState> refillStateMap;
    @Nullable
    protected int[] allowedFluidIds;
    @Nullable
    protected Int2ObjectMap<String> fluidToState;

    protected int[] getAllowedFluidIds() {
        if (this.allowedFluidIds != null) {
            return this.allowedFluidIds;
        } else {
            this.allowedFluidIds = this.refillStateMap
                    .values()
                    .stream()
                    .map(FillContainerInteraction.RefillState::getAllowedFluids)
                    .flatMap(Arrays::stream)
                    .mapToInt(key -> Fluid.getAssetMap().getIndex(key))
                    .sorted()
                    .toArray();
            return this.allowedFluidIds;
        }
    }

    protected Int2ObjectMap<String> getFluidToState() {
        if (this.fluidToState != null) {
            return this.fluidToState;
        } else {
            this.fluidToState = new Int2ObjectOpenHashMap<>();
            this.refillStateMap.forEach((s, refillState) -> {
                for (String sx : refillState.getAllowedFluids()) {
                    this.fluidToState.put(Fluid.getAssetMap().getIndex(sx), s);
                }
            });
            return this.fluidToState;
        }
    }

    @Override
    protected void firstRun(@NonNullDecl InteractionType type, @NonNullDecl InteractionContext context, @NonNullDecl CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandbuffer = context.getCommandBuffer();

        assert commandbuffer != null;

        InteractionSyncData interactionsyncdata = context.getState();
        World world = commandbuffer.getExternalData().getWorld();
        Ref<EntityStore> ref = context.getEntity();
        Ref<EntityStore> ref1 = context.getTargetEntity();
        if (ref1 != null) {
            context.getState().state = InteractionState.Failed;
        } else {
            Player player = commandbuffer.getComponent(ref, Player.getComponentType());
            if (player == null) {
                interactionsyncdata.state = InteractionState.Failed;
            } else {
                Inventory inventory = player.getInventory();
                if (inventory == null) {
                    interactionsyncdata.state = InteractionState.Failed;
                } else {
                    TransformComponent transformcomponent = commandbuffer.getComponent(ref, TransformComponent.getComponentType());
                    if (transformcomponent == null) {
                        interactionsyncdata.state = InteractionState.Failed;
                    } else {
                        HeadRotation headrotation = commandbuffer.getComponent(ref, HeadRotation.getComponentType());
                        if (headrotation == null) {
                            interactionsyncdata.state = InteractionState.Failed;
                        } else {
                            ModelComponent modelcomponent = commandbuffer.getComponent(ref, ModelComponent.getComponentType());
                            if (modelcomponent == null) {
                                interactionsyncdata.state = InteractionState.Failed;
                            } else {
                                ItemStack itemstack = context.getHeldItem();
                                if (itemstack == null) {
                                    interactionsyncdata.state = InteractionState.Failed;
                                } else {
                                    InteractionConfiguration interactionconfiguration = itemstack.getItem().getInteractionConfig();
                                    float f = interactionconfiguration.getUseDistance(player.getGameMode());
                                    Vector3d vector3d = transformcomponent.getPosition().clone();
                                    vector3d.y = vector3d.y + modelcomponent.getModel().getEyeHeight(ref, commandbuffer);
                                    Vector3d vector3d1 = headrotation.getDirection();
                                    Vector3d vector3d2 = vector3d.clone().add(vector3d1.scale(f));
                                    AtomicBoolean atomicboolean = new AtomicBoolean(false);
                                    BlockIterator.iterateFromTo(
                                            vector3d,
                                            vector3d2,
                                            (x, y, z, _, _, _, _, _, _) -> {
                                                Ref<ChunkStore> ref2 = world.getChunkStore()
                                                        .getChunkSectionReference(
                                                                ChunkUtil.chunkCoordinate(x), ChunkUtil.chunkCoordinate(y), ChunkUtil.chunkCoordinate(z)
                                                        );
                                                if (ref2 == null) {
                                                    return true;
                                                } else {
                                                    BlockSection blocksection = ref2.getStore().getComponent(ref2, BlockSection.getComponentType());
                                                    if (blocksection == null) {
                                                        return true;
                                                    } else if (FluidTicker.isSolid(BlockType.getAssetMap().getAsset(blocksection.get(x, y, z)))) {
                                                        interactionsyncdata.state = InteractionState.Failed;
                                                        return false;
                                                    } else {
                                                        FluidSection fluidsection = ref2.getStore().getComponent(ref2, FluidSection.getComponentType());
                                                        if (fluidsection == null) {
                                                            return true;
                                                        } else {
                                                            int i = fluidsection.getFluidId(x, y, z);
                                                            int[] aint = this.getAllowedFluidIds();
                                                            if (aint != null && Arrays.binarySearch(aint, i) < 0) {
                                                                interactionsyncdata.state = InteractionState.Failed;
                                                                return true;
                                                            } else {
                                                                String s = this.getFluidToState().get(i);
                                                                if (s == null) {
                                                                    interactionsyncdata.state = InteractionState.Failed;
                                                                    return false;
                                                                } else {
                                                                    ItemStack itemstack1 = context.getHeldItem();
                                                                    Item item = itemstack1.getItem().getItemForState(s);
                                                                    if (item == null) {
                                                                        interactionsyncdata.state = InteractionState.Failed;
                                                                        return false;
                                                                    } else {
                                                                        FillContainerInteraction.RefillState refillcontainerinteraction$refillstate = this.refillStateMap
                                                                                .get(s);
                                                                        if (item.getId().equals(itemstack1.getItemId())) {
                                                                            if (refillcontainerinteraction$refillstate != null) {
                                                                                if (itemstack1.getDurability() + refillcontainerinteraction$refillstate.durability > itemstack1.getMaxDurability()) {
                                                                                    interactionsyncdata.state = InteractionState.Failed;
                                                                                    return false;
                                                                                }

                                                                                ItemStack itemstack3 = itemstack1.withIncreasedDurability(refillcontainerinteraction$refillstate.durability);
                                                                                ItemStackSlotTransaction itemstackslottransaction = context.getHeldItemContainer()
                                                                                        .setItemStackForSlot(context.getHeldItemSlot(), itemstack3);
                                                                                if (!itemstackslottransaction.succeeded()) {
                                                                                    interactionsyncdata.state = InteractionState.Failed;
                                                                                    return false;
                                                                                }

                                                                                context.setHeldItem(itemstack3);
                                                                                atomicboolean.set(true);
                                                                            }
                                                                        } else {
                                                                            ItemStackSlotTransaction itemstackslottransaction1 = context.getHeldItemContainer()
                                                                                    .removeItemStackFromSlot(context.getHeldItemSlot(), itemstack1, 1);
                                                                            if (!itemstackslottransaction1.succeeded()) {
                                                                                interactionsyncdata.state = InteractionState.Failed;
                                                                                return false;
                                                                            }

                                                                            ItemStack itemstack2 = new ItemStack(item.getId(), 1);
                                                                            if (refillcontainerinteraction$refillstate != null
                                                                                && refillcontainerinteraction$refillstate.durability > 0.0) {
                                                                                itemstack2 = itemstack2.withDurability(
                                                                                        refillcontainerinteraction$refillstate.durability
                                                                                );
                                                                            }

                                                                            if (itemstack1.getQuantity() == 1) {
                                                                                ItemStackSlotTransaction itemstackslottransaction2 = context.getHeldItemContainer()
                                                                                        .setItemStackForSlot(context.getHeldItemSlot(), itemstack2);
                                                                                if (!itemstackslottransaction2.succeeded()) {
                                                                                    interactionsyncdata.state = InteractionState.Failed;
                                                                                    return false;
                                                                                }

                                                                                context.setHeldItem(itemstack2);
                                                                            } else {
                                                                                SimpleItemContainer.addOrDropItemStack(
                                                                                        commandbuffer, ref, inventory.getCombinedHotbarFirst(), itemstack2
                                                                                );
                                                                                context.setHeldItem(
                                                                                        context.getHeldItemContainer().getItemStack(context.getHeldItemSlot())
                                                                                );
                                                                            }
                                                                        }

                                                                        if (refillcontainerinteraction$refillstate != null
                                                                            && refillcontainerinteraction$refillstate.getTransformFluid() != null) {
                                                                            int j = Fluid.getFluidIdOrUnknown(
                                                                                    refillcontainerinteraction$refillstate.getTransformFluid(),
                                                                                    "Unknown fluid %s",
                                                                                    refillcontainerinteraction$refillstate.getTransformFluid()
                                                                            );
                                                                            boolean flag = fluidsection.setFluid(
                                                                                    x, y, z, j, (byte) Fluid.getAssetMap().getAsset(j).getMaxFluidLevel()
                                                                            );
                                                                            if (!flag) {
                                                                                interactionsyncdata.state = InteractionState.Failed;
                                                                            }

                                                                            world.performBlockUpdate(x, y, z);
                                                                            atomicboolean.set(true);
                                                                        }

                                                                        return false;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                    );
                                    if (!atomicboolean.get()) {
                                        context.getState().state = InteractionState.Failed;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Nonnull
    @Override
    public String toString() {
        return "RefillContainerInteraction{refillStateMap="
               + this.refillStateMap
               + ", allowedBlockIds="
               + Arrays.toString(this.allowedFluidIds)
               + ", blockToState="
               + this.fluidToState
               + "} "
               + super.toString();
    }

    protected static class RefillState {
        public static final BuilderCodec<FillContainerInteraction.RefillState> CODEC = BuilderCodec.builder(
                        FillContainerInteraction.RefillState.class, FillContainerInteraction.RefillState::new
                )
                .append(
                        new KeyedCodec<>("AllowedFluids", new ArrayCodec<>(Codec.STRING, String[]::new)),
                        (interaction, v) -> interaction.allowedFluids = v,
                        interaction -> interaction.allowedFluids
                )
                .addValidator(Validators.nonNull())
                .add()
                .addField(
                        new KeyedCodec<>("TransformFluid", Codec.STRING),
                        (interaction, v) -> interaction.transformFluid = v,
                        interaction -> interaction.transformFluid
                )
                .addField(
                        new KeyedCodec<>("Durability", Codec.DOUBLE), (interaction, v) -> interaction.durability = v, interaction -> interaction.durability
                )
                .build();

        protected String[] allowedFluids;
        protected String transformFluid;
        protected double durability = 1.0; // Luke100000: Set this to 1

        public String[] getAllowedFluids() {
            return this.allowedFluids;
        }

        public String getTransformFluid() {
            return this.transformFluid;
        }

        public double getDurability() {
            return this.durability;
        }

        @Nonnull
        @Override
        public String toString() {
            return "RefillState{allowedFluids="
                   + Arrays.toString(this.allowedFluids)
                   + ", transformFluid='"
                   + this.transformFluid
                   + "', durability="
                   + this.durability
                   + "}";
        }
    }
}
