import { defineConfig } from 'vitest/config';

/**
 * A lefedettségi küszöb itt kényszerül ki, nem a CI parancsában — így egy
 * helyen van, és lokálisan ugyanaz bukik el, mint a CI-ben. A mérendő fájlok
 * körét az Angular builder adja; itt csak a küszöböt írjuk elő.
 */
export default defineConfig({
  test: {
    coverage: {
      thresholds: {
        lines: 80,
        statements: 80,
        branches: 80,
      },
    },
  },
});
