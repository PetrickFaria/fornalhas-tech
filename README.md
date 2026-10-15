# Fornalhas Tech — projeto inicial

Projeto-fonte direcionado a Minecraft Java 1.19.2, Forge 43.5.0 e Java 17. Mod ID: `fornalhastech`.

## O que está implementado no código desta entrega

- Um bloco registrado `tech_furnace` e oito itens registrados: seis núcleos e dois módulos.
- Receitas de fabricação para o bloco, os seis núcleos e os módulos, com os padrões das referências.
- Uma entidade de bloco com inventário persistente de seis posições (entrada, combustível, saída, núcleo, módulo e aprimorador reservado).
- Menu próprio com filtros para os slots especiais de núcleo, módulo e aprimorador; slot do módulo aceita somente Alto-Forno ou Defumador.
- Seleção do tipo de receita por módulo (smelting, blasting e smoking), cálculo de avanço pela duração definida na receita e multiplicador do núcleo (1x, 2x, 3x, 5x, 7x, 9x, 11x).
- Consumo de combustível usando as regras Forge, preservação de componentes no NBT, devolução de conteúdo ao quebrar o bloco e emissão de luz baseada em estado.
- Recursos de textura pixel art iniciais, ícones para os oito itens e GUI base. Referências fornecidas estão preservadas em `reference/`.

## Pendências que impedem chamar isto de versão pronta

Este ambiente não possui Gradle instalado. O wrapper configurado precisa baixar Gradle 7.5.1 do servidor de distribuições, que redirecionou para GitHub e expirou durante esta execução. Portanto, a compilação Forge não foi concluída e **não há JAR instalável nesta entrega**.

A implementação também precisa de uma etapa de compilação e correção de API antes de ser considerada funcional: ainda faltam renderização tridimensional/emissiva por núcleo e modo, tela de seleção de cores com sincronização, sincronização visual de progresso/combustível, experiência por receita e testes dentro do Minecraft. As seis opções de cor ainda não podem ser escolhidas pela interface. O modelo atual é uma textura cúbica inicial e não corresponde ao relevo 3D final das imagens.

Não instale como mod pronto. Para tentar compilar em um ambiente com acesso ao Gradle/Forge Maven: `./gradlew build` (Java 17). O resultado esperado, após resolver eventuais erros de compilação, será `build/libs/fornalhas-tech-1.0.0.jar`.
