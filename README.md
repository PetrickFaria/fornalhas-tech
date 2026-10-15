# Fornalhas Tech — estado da implementação

Projeto para Minecraft Java 1.19.2, Forge 43.5.0 e Java 17. Mod ID: `fornalhastech`.

## O que está no projeto

- Um bloco `tech_furnace` e oito itens: seis núcleos e os módulos Alto-Forno e Defumador.
- Receitas de fabricação para o bloco e os oito itens.
- Inventário persistido de seis posições, slots dedicados a núcleo/módulo, consumo de combustível Forge e devolução dos itens ao quebrar o bloco.
- Seleção das receitas de smelting, blasting ou smoking pelo módulo inserido.
- Multiplicadores de progresso configurados para os seis núcleos: 1x, 2x, 3x, 5x, 7x, 9x e 11x contando sem núcleo como 1x.
- Workflow do GitHub Actions em `.github/workflows/build.yml`, com Java 17, Gradle Wrapper 7.5.1, ForgeGradle 5.1 e `workflow_dispatch`.

## Pendências funcionais/visuais

Este projeto ainda não implementa todos os requisitos visuais e de jogo e não deve ser apresentado como versão final:

- O modelo do bloco é cúbico e não reproduz o relevo 3D das imagens.
- As texturas são versões iniciais, não a reprodução fiel das 15 referências; não há emissão luminosa seletiva dos detalhes coloridos.
- A luz atual é um valor fixo no estado aceso. Não existe iluminação dinâmica por tier/cor.
- Não há tela secundária para escolher cores nem sincronização de cor entre cliente e servidor.
- O multiplicador de velocidade está no tick do bloco. O valor de experiência da receita não é acumulado nem entregue ao jogador.
- O menu não sincroniza barras de progresso/combustível completas. O funcionamento precisa de testes em uma instância real de Minecraft.

As 15 imagens conceituais estão no diretório `reference/` do pacote-fonte. Elas são referências, não texturas já incorporadas ao mod.

## Compilação

`./gradlew --no-daemon --stacktrace clean build` (Java 17). O workflow validou compilação e gerou `fornalhas-tech-1.0.0.jar` no artefato da execução #3. Uma compilação bem-sucedida não comprova carregamento, balanceamento ou funcionamento dentro do jogo.
