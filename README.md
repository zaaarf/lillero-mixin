# WIP, IT DOES NOT WORK YET!

# Lillero-mixin
Lillero-mixin is a Mixin plugin capable of applying [Lillero](https://github.com/zaaarf/lillero) ASM patches without
needing to inject itself as a JAR library. While slightly dirtier code-wise, this has the key advantage over the older
[Lillero-loader](https://github.com/zaaarf/lillero-loader) of being compatible with both Forge and Fabric - and, barring
major API changes, with any other future mod loader that will try to force Mixin on you.

## Credits
This time there's one other project that must be mentioned. I would've never thought of this had I not stumbled on
[Manningham Mills](https://github.com/Chocohead/Fabric-ASM). So, thanks to Chocohead for showing that it was indeed
possible to work on mixin-centric systems (Fabric especially) without writing a separate loader!
