package ftbsc.lll.mixin;

import ftbsc.lll.IInjector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.*;

/**
 * Allows you to load your mod's Lillero patches as a Mixin plugin.
 * You must extend this and register your child class as a plugin in your
 * mod's Mixin config; refer to your mod loader's instructions for details.
 * For this to work, Mixin must know what the target classes are; you can do
 * so by creating an empty interface with a {@link Mixin} annotation listing
 * them; if it's available in your environment, Lillero-processor can generate
 * it for you.
 */
public abstract class LilleroMixinPlugin implements IMixinConfigPlugin {
	/**
	 * The JVM arg key which specifies the logging level for this.
	 */
	private static final String LEVEL_KEY = "lll.logging.level";

	/**
	 * Maps each fully-qualified name to its associated class.
	 */
	protected final Map<String, List<IInjector>> injectorMap = new HashMap<>();

	/**
	 * The logger that this loader uses.
	 */
	protected final Logger logger;

	/**
	 * Constructor which takes a custom logger.
	 * @param logger the logger to use for this
	 */
	protected LilleroMixinPlugin(Logger logger) {
		this.logger = logger;
	}

	/**
	 * Constructor which uses the default logger.
	 */
	protected LilleroMixinPlugin() {
		this.logger = Configurator.setLevel(
			LogManager.getLogger(this.getClass().getCanonicalName()),
			Level.toLevel(System.getProperty(LEVEL_KEY), Level.INFO)
		);
	}

	/**
	 * Gets the Lillero injectors this is supposed to apply.
	 * This may be overridden to add custom loading behaviour.
	 * @return an iterable sequence of {@link IInjector}s.
	 */
	protected Iterable<IInjector> getInjectors() {
		return ServiceLoader.load(IInjector.class, this.getClass().getClassLoader());
	}

	@Override
	public void onLoad(String mixinPackage) {
		int found = 0;
		for(IInjector inj : this.getInjectors()) {
			if(!inj.getClass().getPackage().getName().equals(mixinPackage)) {
				// stupid fixes for stupid problems
				continue;
			}

			this.logger.debug("Found injector {}!", inj.getClass().getSimpleName());
			this.injectorMap.computeIfAbsent(inj.targetClass(), k -> new ArrayList<>()).add(inj);
			found++;
		}

		this.logger.debug("Found {} injectors in plugin {}!", found, this.getClass().getCanonicalName());
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override // don't care, just say yes
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return true;
	}

	@Override // we don't need to alter the target class list
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override // no need
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String className, ClassNode clazz, String mixinClassName, IMixinInfo mixinInfo) {
		this.applyPatches(className, clazz);
	}

	@Override
	public void postApply(String className, ClassNode clazz, String mixinClassName, IMixinInfo mixinInfo) {}

	/**
	 * Applies the appropriate Lillero patches given a node and a class name.
	 * @param className the class' fully qualified name
	 * @param classNode the target class
	 */
	protected void applyPatches(String className, ClassNode classNode) {
		List<IInjector> injectors = this.injectorMap.remove(className); // remove so it's only once
		if(injectors != null) {
			this.logger.debug("Found {} injectors for class {}", injectors.size(), className);
			try {
				Set<IInjector> notFound = new HashSet<>(injectors);
				for(final MethodNode methodNode : classNode.methods) {
					injectors.stream()
						.filter(i -> i.methodName().equals(methodNode.name) && i.methodDesc().equals(methodNode.desc))
						.forEach(inj -> {
							try {
								this.logger.debug(
									"Beginning transformation of method {}::{} with reason \"{}\"!",
									className,
									methodNode.name,
									inj.reason()
								);
								inj.inject(classNode, methodNode);
								notFound.remove(inj);
								this.logger.debug(
									"Successfully transformed method {}::{} with reason \"{}\"!",
									className,
									methodNode.name,
									inj.reason()
								);
							} catch(Throwable t) {
								this.logger.error(
									"{} thrown from {}::{} for the task with the description \"{}\"!.",
									t.getClass().getSimpleName(),
									className,
									methodNode.name,
									inj.reason()
								);
								this.logger.error(t.getMessage(), t);
							}
						});
				}

				for(IInjector inj : notFound) {
					this.logger.warn(
						"Injector (reason: \"{}\") for method {}::{} with descriptor {} did not find a target!",
						inj.targetClass(),
						inj.methodName(),
						inj.methodDesc(),
						inj.reason()
					);
				}
			} catch(Throwable t) {
				this.logger.error(
					"{} thrown from transforming class {}!",
					t.getClass().getSimpleName(),
					className
				);
				this.logger.error(t.getMessage(), t);
			}
		}
	}
}
