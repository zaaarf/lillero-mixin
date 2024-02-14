package ftbsc.lll.mixin;

import ftbsc.lll.IInjector;
import ftbsc.lll.exceptions.InjectionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.*;

/**
 * Allows you to load your mod's Lillero patches as a Mixin plugin.
 * Extend this class and specify the child as a plugin in your mod's Mixin
 * config. Refer to your mod loader's instructions for details on how this
 * is done.
 * Methods are left non-final in case you want to alter their behaviour in
 * any way, but I can't really see any merit in doing so.
 */
public abstract class LilleroMixinPlugin implements IMixinConfigPlugin {
	/**
	 * The {@link Logger} used by this library.
	 */
	protected static final Logger LOGGER = LogManager.getLogger(LilleroMixinPlugin.class);

	/**
	 * Maps each fully-qualified name to its associated class.
	 */
	private final Map<String, List<IInjector>> injectorMap = new HashMap<>();

	/**
	 * Whether Lillero should take precedence over regular mixins.
	 */
	private final boolean precedence;

	/**
	 * The constructor.
	 * @param precedence whether Lillero should take precedence over regular mixins
	 */
	public LilleroMixinPlugin(boolean precedence) {
		this.precedence = precedence;
	}

	/**
	 * Called after the plugin is instantiated, do any setup here.
	 * @param mixinPackage The mixin root package from the config
	 */
	@Override
	public void onLoad(String mixinPackage) {
		for(IInjector inj : ServiceLoader.load(IInjector.class, this.getClass().getClassLoader())) {
			LOGGER.info("Registering injector {}", inj.name());
			List<IInjector> injectors = this.injectorMap.get(inj.targetClass());
			if(injectors == null) {
				injectors = new ArrayList<>();
				injectorMap.put(inj.targetClass(), injectors);
			}
			injectors.add(inj);
		}
	}

	/**
	 * Returns null, so it's effectively ignored.
	 * @return always null
	 */
	@Override
	public String getRefMapperConfig() {
		return null;
	}

	/**
	 * Tells Mixin to always apply these patches.
	 * Lillero doesn't support conditional patches: any check should happen
	 * within the patch code itself, with the patch code's scope.
	 * @param targetClassName fully qualified class name of the target class
	 * @param mixinClassName fully qualified class name of the mixin
	 * @return always true
	 */
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return true;
	}

	/**
	 * Does nothing, as we don't need to alter the target class list.
	 * @param myTargets target class set from the companion config
	 * @param otherTargets target class set incorporating targets from all other
	 *                     configs, read-only
	 */
	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	/**
	 * This does not apply any additional mixins.
	 * @return always null
	 */
	@Override
	public List<String> getMixins() {
		return null;
	}

	/**
	 * Called immediately before a mixin is applied to a target class.
	 * Will apply Lillero patches if {@link #precedence} is true.
	 * @param className transformed name of the target class
	 * @param clazz target class tree
	 * @param mixinClassName name of the mixin class
	 * @param mixinInfo information about this mixin
	 */
	@Override
	public void preApply(String className, ClassNode clazz, String mixinClassName, IMixinInfo mixinInfo) {
		if(precedence) this.applyLilleroPatches(className, clazz);
	}

	/**
	 * Called immediately after a mixin is applied to a target class.
	 * Will apply Lillero patches if {@link #precedence} is false.
	 * @param className transformed name of the target class
	 * @param clazz target class tree
	 * @param mixinClassName name of the mixin class
	 * @param mixinInfo information about this mixin
	 */
	@Override
	public void postApply(String className, ClassNode clazz, String mixinClassName, IMixinInfo mixinInfo) {
		if(!precedence) this.applyLilleroPatches(className, clazz);
	}

	/**
	 * Applies the appropriate Lillero patches given a node and a class name.
	 * @param className the class' fully qualified name
	 * @param clazz the target class
	 */
	protected void applyLilleroPatches(String className, ClassNode clazz) {
		List<IInjector> injectors = this.injectorMap.remove(className); // remove so it's only once
		if(injectors != null) {
			injectors.forEach((inj) -> clazz.methods.stream()
				.filter(m -> m.name.equals(inj.methodName()) && m.desc.equals(inj.methodDesc()))
				.forEach(m -> {
					try {
						LOGGER.info(
							"Patching {}.{} with {} ({})",
							className, m.name,
							inj.name(),
							inj.reason());
						inj.inject(clazz, m);
					} catch (InjectionException exception) {
						LOGGER.error("Error applying patch '{}' : {}", inj.name(), exception);
					}
				}));
		}
	}
}
