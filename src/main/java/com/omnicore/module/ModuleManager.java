package com.omnicore.module;

import com.omnicore.module.combat.*;
import com.omnicore.module.movement.*;
import com.omnicore.module.survival.*;
import com.omnicore.module.building.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    
    public void registerModules() {
        // Combat modules
        registerModule(new KillAuraModule());
        registerModule(new CrystalPvPModule());
        registerModule(new SpearCombatModule());
        registerModule(new AutoDodgeModule());
        registerModule(new AutoShieldModule());
        
        // Movement modules
        registerModule(new PathfindingModule());
        registerModule(new FollowModule());
        registerModule(new ElytraFlyModule());
        registerModule(new MountControlModule());
        registerModule(new AutoWalkModule());
        
        // Survival modules
        registerModule(new AutoFarmModule());
        registerModule(new AutoMineModule());
        registerModule(new AutoFishModule());
        registerModule(new AutoEatModule());
        registerModule(new AutoCraftModule());
        
        // Building modules
        registerModule(new SchematicBuilderModule());
        registerModule(new AutoBridgeModule());
        registerModule(new AutoPillarModule());
    }
    
    public void registerModule(Module module) {
        modules.add(module);
    }
    
    public List<Module> getModules() {
        return modules;
    }
    
    public Optional<Module> getModule(String name) {
        return modules.stream()
            .filter(m -> m.getName().equalsIgnoreCase(name))
            .findFirst();
    }
    
    public <T extends Module> Optional<T> getModule(Class<T> clazz) {
        return modules.stream()
            .filter(clazz::isInstance)
            .map(clazz::cast)
            .findFirst();
    }
    
    public void onTick() {
        modules.stream()
            .filter(Module::isEnabled)
            .forEach(Module::onTick);
    }
    
    public void onRender() {
        modules.stream()
            .filter(Module::isEnabled)
            .forEach(Module::onRender);
    }
}
