# Commit Message Format

This project uses an Angular inspired commit message format, adapted to this codebase.
It keeps history readable and supports automated changelogs.

Each commit message has a header, a body, and a footer.

```
<header>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

The header is required and must follow the Commit Message Header format.

The body is required for all commits except type "docs". When present, the body must be at
least 20 characters long.

The footer is optional.

## Commit Message Header

```
<type>(<scope>): <short summary>
```

- Summary in present tense. Not capitalized. No period at the end.
- Scope: free form, keep it short and specific.
- Type: build, ci, docs, feat, fix, perf, refactor, test.

Examples:

- feat(easing): add easing strategy and standard set
- fix(sequencing): drain trailing instant steps on completion
- build(engine): wire junit 5 test runner

## Type

| Type     | Description                                               |
| -------- | --------------------------------------------------------- |
| build    | Changes to build system or external dependencies          |
| ci       | Changes to CI configuration or scripts                    |
| docs     | Documentation only changes                                |
| feat     | A new feature                                             |
| fix      | A bug fix                                                 |
| perf     | A code change that improves performance                   |
| refactor | A code change that neither fixes a bug nor adds a feature |
| test     | Adding missing tests or correcting existing tests         |

## Scope

Use a short, specific scope that matches the area of change. Keep it consistent across the
project. Examples: engine, easing, curve, sequencing, emitter, beam, render, network,
registry, build, docs, ci.

## Body

Use the imperative, present tense. Explain why the change is needed. If helpful, include the
previous behavior and the new behavior. Minimum length is 20 characters.

## Footer

The footer is optional. Use it for breaking changes, deprecations, and issue references.

```
BREAKING CHANGE: short summary

Detailed description and migration steps.

Fixes #123
```

## Revert commits

```
revert: <header>
```

The body must include the reverted commit SHA and a clear reason for the revert.
