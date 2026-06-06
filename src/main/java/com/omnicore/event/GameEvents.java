package com.omnicore.event;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;

public class GameEvents {

    public static class TickEvent extends Event {
        public static final TickEvent INSTANCE = new TickEvent();
    }

    public static class RenderEvent extends Event {
        public final MatrixStack matrices;
        public final float tickDelta;

        public RenderEvent(MatrixStack matrices, float tickDelta) {
            this.matrices = matrices;
            this.tickDelta = tickDelta;
        }
    }

    public static class AttackEntityEvent extends Event {
        public final Entity target;

        public AttackEntityEvent(Entity target) {
            this.target = target;
        }
    }

    public static class SendPacketEvent extends Event {
        public final Packet<?> packet;

        public SendPacketEvent(Packet<?> packet) {
            this.packet = packet;
        }
    }

    public static class ReceivePacketEvent extends Event {
        public final Packet<?> packet;

        public ReceivePacketEvent(Packet<?> packet) {
            this.packet = packet;
        }
    }

    public static class KeyPressEvent extends Event {
        public final int keyCode;

        public KeyPressEvent(int keyCode) {
            this.keyCode = keyCode;
        }
    }
}
