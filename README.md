# CoinsEconomy

Plugin de economia para Spigot/Paper, com **coins** como moeda do servidor.
Ele se registra como provedor de `Economy` do **[Vault](https://github.com/milkbowl/Vault)**
(assim qualquer plugin compatível com Vault — lojas, ranks pagos, minigames, etc.
consegue usar os coins automaticamente) e usa um armazenamento simples em
arquivo, inspirado no **[HSGamer/BetterEconomy](https://github.com/HSGamer/BetterEconomy)**.

## Requisitos

| Item | Versão |
|---|---|
| Minecraft | **26.2** (build feito com `spigot-api:26.2-R0.1-SNAPSHOT`) |
| Servidor | Spigot ou Paper 26.2 |
| Java (servidor) | **25 ou superior** (26.2 exige Java 25+; Java 26 também funciona) |
| Java (build) | JDK 21+ (compila com `--release 21` para máxima compatibilidade) |
| Dependência | [Vault](https://www.spigotmc.org/resources/vault.34315/) instalado no servidor |

> O `pom.xml` compila com `--release 21` de propósito: o bytecode gerado roda
> sem problemas em qualquer JVM 21, 25 ou 26. Você pode compilar tanto com o
> seu JDK 26 quanto com um JDK 21, o resultado é o mesmo `.jar`.

## Como compilar

```bash
mvn clean package
```

O arquivo final aparece em `target/CoinsEconomy-1.0.0.jar`. Coloque-o, junto
com o `Vault.jar`, dentro da pasta `plugins/` do servidor.

## Comandos

| Comando | Descrição | Permissão | Padrão |
|---|---|---|---|
| `/coins` | Mostra seu próprio saldo | `coinseconomy.coins` | todos |
| `/coins <jogador>` | Mostra o saldo de outro jogador | `coinseconomy.coins` | todos |
| `/coins top` | Atalho para `/topcoins` | `coinseconomy.top` | todos |
| `/coins give <jogador> <quantidade>` | Adiciona coins na conta de alguém | `coinseconomy.admin` | op |
| `/coins set <jogador> <quantidade>` | Define o saldo de alguém | `coinseconomy.admin` | op |
| `/pagar <jogador> <quantidade>` | Transfere coins do seu saldo para outro jogador | `coinseconomy.pagar` | todos |
| `/cobrar <jogador> <quantidade>` | Remove (cobra) uma quantidade de coins de um jogador | `coinseconomy.cobrar` | op |
| `/topcoins` (aliases `/coinstop`, `/baltop`) | Abre uma GUI com o ranking dos 10 jogadores com mais coins, mostrando a cabeça de cada um | `coinseconomy.top` | todos |

### Sobre o `/cobrar`

Ele foi implementado como um comando **administrativo**: remove coins do
saldo de um jogador (por exemplo, para aplicar uma multa). Ele não transfere
o valor para quem executou o comando — os coins são simplesmente retirados
da economia. Se você preferia um `/cobrar` que funcionasse como uma cobrança
entre jogadores (A "cobra" e B precisa aceitar pagar), é só avisar que dá
para adaptar.

### Permissão coringa

`coinseconomy.*` concede acesso a todos os comandos acima de uma vez
(útil para dar a um grupo de administradores).

## Configuração (`config.yml`)

```yaml
saldo-inicial: 0.0

moeda:
  singular: 'coin'
  plural: 'coins'

cobranca-permite-saldo-negativo: false

salvamento-automatico-ticks: 6000
```

## Armazenamento dos dados

Os saldos ficam em `plugins/CoinsEconomy/data.yml`, indexados por UUID do
jogador (para sobreviver a trocas de nome). Os dados são salvos:

- automaticamente a cada `salvamento-automatico-ticks` (padrão: 5 minutos),
  de forma assíncrona;
- ao desligar o servidor (`onDisable`).

## Limitações conhecidas / próximos passos possíveis

- Armazenamento apenas em arquivo YAML (sem suporte nativo a MySQL/SQLite —
  dá para adicionar um `StorageProvider` se o servidor crescer muito).
- Não implementa contas bancárias (`hasBankSupport()` retorna `false`),
  já que o pedido era um sistema de coins por jogador.
- Mensagens usam `ChatColor` (formato "legado") para manter o código simples;
  dá para migrar para a Adventure API (`net.kyori.adventure`) se quiser
  hex colors ou componentes de texto mais avançados.
