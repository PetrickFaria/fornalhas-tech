# Fornalhas Tech

Mod para Minecraft Java 1.19.2, Forge 43.5.0 e Java 17. Mod ID: `fornalhastech`.

## Recursos incluídos

- Um bloco `tech_furnace`, seis Núcleos Tech e dois módulos (Alto-Forno e Defumador).
- Receitas de fabricação, recursos de bloco/itens, blockstates com 32 combinações de estado e traduções pt-BR.
- Inventário persistente com slots de entrada, combustível, saída, núcleo, módulo e aprimorador reservado.
- Os módulos escolhem smelting, blasting e smoking. Os níveis de núcleo ajustam o progresso com os multiplicadores definidos no código.
- Interface de 176×220 px com três slots especiais (verde/amarelo/vermelho), grelha do inventário do jogador, medidor de combustível e indicador de progresso sincronizados, com preenchimento limitado à faixa da textura.
- `pack.mcmeta` para Minecraft 1.19.2. A GUI usa UVs baseadas no tamanho real de 176×220.

## Estado conhecido

A interface foi corrigida conforme a captura descrita: textura legível, dimensões e UVs alinhados, posições de slots consistentes, traduções presentes e recursos no JAR. Os modelos JSON, blockstates, imagens e referências de textura foram verificados dentro do JAR.

O mod ainda não está completo segundo o conceito visual original. O modelo do bloco permanece cúbico, as texturas ainda não reproduzem fielmente as 15 referências, não há emissão seletiva dos detalhes coloridos ou iluminação dinâmica por núcleo, a interface secundária para seleção de cores não foi implementada e a experiência das receitas ainda não é entregue. Também não validei esta versão dentro de uma instância do Minecraft.

As imagens conceituais estão no diretório `reference/` do pacote-fonte. Elas são referências de design, não texturas finais prontas.

## Compilar

`./gradlew --no-daemon --stacktrace clean build` (Java 17). O workflow GitHub Actions usa Ubuntu, Gradle Wrapper 7.5.1 e ForgeGradle `[5.1,5.2)`, e publica `build/libs/*.jar` como artefato.

## Versionamento

A versão única do mod fica em `mod_version` no `gradle.properties`. Em cada nova atualização entregue, incremente o último componente (por exemplo, `1.0.4` para `1.0.5`). O Gradle usa essa propriedade para o nome do JAR e para `mods.toml`; o GitHub Actions lê a mesma propriedade para nomear o artefato.


### Update 1.0.12
The main furnace interface and the color customization view now use the supplied pixel-art references. Core tiers progressively unlock black, red, white, yellow, green, cyan, and brown visual choices. Colors are server-validated. The new 64x64 face PNGs are retained under `assets/fornalhastech/textures/block/source/`; the red `(237,28,36)` pixels are the only pixels extracted as tinted emissive masks. The base model uses 0.001-unit-thick emissive inlay faces recessed behind the shell, an inset of 0.125 model units, edge rails with `r=0.125`, and vertex caps with a `2r=0.25` outward relief.

Fuel now generates stored Forge Energy (32,000 FE capacity, 10 FE per vanilla fuel burn tick). Smelting consumes 10 FE per progress tick, scaled by the existing core speed; stored energy is saved in block-entity NBT. Modules remain available through quick-move and sneaking interaction because the referenced main GUI has no module slot. See `RELATORIO_TESTES.txt` for verification limits.
