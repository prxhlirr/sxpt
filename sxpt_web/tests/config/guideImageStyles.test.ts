import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

const styles = readFileSync(
  new URL('../../src/styles.css', import.meta.url),
  'utf8'
);

describe('guide image styles', () => {
  it('allows a responsive 1200px popup image to use up to 675px height', () => {
    expect(styles).toMatch(
      /\.guide-bubble\s*\{[^}]*max-width:\s*calc\(100vw\s*-\s*24px\)[^}]*max-height:\s*calc\(100vh\s*-\s*24px\)[^}]*overflow:\s*auto/s
    );
    expect(styles).toMatch(
      /\.guide-bubble__image\s*\{[^}]*max-height:\s*min\(70vh,\s*675px\)[^}]*object-fit:\s*contain/s
    );
    expect(styles).not.toMatch(
      /\.guide-bubble\.compact\s+\.guide-bubble__image\s*\{[^}]*max-height:\s*140px/s
    );
  });
});
