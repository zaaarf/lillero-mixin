# Lillero-mixin
Lillero-mixin is a Mixin plugin capable of applying [Lillero](https://github.com/zaaarf/lillero) ASM patches without
needing to inject itself as a JAR library. While slightly dirtier code-wise, this has the key advantage over the older
[Lillero-loader](https://github.com/zaaarf/lillero-loader) of being compatible with both Forge and Fabric - and, barring
major API changes, with any other future mod loader that will try to force Mixin on you.

## How to use
1. Get this as well as the [core](https://github.com/zaaarf/lillero) into your game classpath. The easiest way is to
	bundle them together with your mod.
2. Write an empty class or interface with a `@Mixin` annotation listing all the classes that will be modified.
	- If you are using the [processor](https://github.com/zaaarf/lillero-processor), this step is considerably simpler;
		just pass the `fakeMixin` compiler argument with the fully qualified name you wish the class to have.
3. Write an appropriate Mixin configuration, specifying the empty Mixin you generated or wrote, and `LilleroMixinPlugin`
	as plugin. A minimal example (for Fabric) is provided below:

```json
{
  "required": true,
  "package": "${fakeMixinPackage}",
  "plugin": "ftbsc.lll.mixin.LilleroMixinPlugin",
  "mixins": ["${fakeMixinClass}"]
}
```

All that's left to do at this point is to tell your mod loader to use that Mixin configuration file; if you are unsure
now how to do this, refer to your mod loader's docs.

## Credits
Lillero-mixin is standing on the shoulders of giants. While my implementation is definitely cleaner, I would've never
thought of this had I not stumbled on [Manningham Mills](https://github.com/Chocohead/Fabric-ASM) (AKA Fabric-ASM).
So, big thanks to [Chocohead](https://github.com/Chocohead) for proving it was possible!