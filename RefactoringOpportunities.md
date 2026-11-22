# Refactoring Opportunities

| Local | Cheiro no código | Refabricação | Nº aluno |
|---|---|---|---|
| `Fleet::addShip` | Complex condition (três condições combinadas numa só expressão) | Introduce Variable | 130497 |
| `Fleet::createRandom` | Magic literals (array `shipTypes` com os nomes dos navios embebido no método) | Introduce Constant | 130497 |
| `Compass::c` (campo, construtor e `getDirection`) | Mysterious Name (nome de uma só letra) | Rename | 130497 |
| `Tasks::menu` | Long Method / Switch Statements (ramo `simula` com ciclo e `try/catch` aninhados) | Extract Method | 130497 |
| `Game::getRemainingShips` | Temporary variable redundante (`floatingShips` usada uma só vez) | Inline Variable | 130497 |
| `Main::main` | Speculative Generality (declara `IOException`, `UnsupportedAudioFileException` e `LineUnavailableException`, que nunca são lançadas) | Change Signature (Remove Thrown Exceptions) | 130497 |
| `Fleet::printShips` | Feature Envy (não usa estado da instância) | Make Static | 130497 |
| `Ship::pos` (campo) | Mysterious Name (nome do campo escondido pelos parâmetros `pos` de `occupies`, `tooCloseTo` e `shoot`) | Rename | 131635 |
| `SoundManager::loadSound`, `SoundManager::getDuration` | Duplicated Code (mesma procura do recurso e mesma exceção "Sound not found") | Extract Method | 131635 |
| `Move::processEnemyFire` | Magic literals (`" tiro"` repetido 6 vezes) | Introduce Constant | 131635 |
| `Game::printUIBoard` | Speculative Generality (parâmetro `showLegend` nunca usado) | Change Signature (Remove Parameter) | 131635 |
| `Ship::getAdjacentPositions` | Temporary variable redundante (`adjacents` usada uma só vez) | Inline Variable | 131635 |
| `Ship::positions` (campo) | Encapsulation (campo `protected` exposto, apesar de já existir `getPositions()`) | Encapsulate Fields | 131635 |
| `Ship::isInsideBoard` | Complex condition (`Game.BOARD_SIZE - 1` repetido duas vezes na mesma expressão) | Introduce Variable | 131635 |
