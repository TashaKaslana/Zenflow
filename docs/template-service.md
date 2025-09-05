# Template Service

Zenflow resolves dynamic values through **TemplateService**, which uses the [Aviator](https://github.com/killme2008/aviator) expression engine. Templates wrap expressions in `{{` and `}}` and are resolved against the current `ExecutionContext`.

## Expression syntax
- `{{ 1 + 1 }}` evaluates a single expression and preserves its type (`2` as a number).
- Text with embedded expressions is interpolated: `"Hello {{ get('user.name') }}"` -> `"Hello Alice"`.

## Accessing workflow data
- Use the `get` function to read values from the context: `{{ get('node1.output.total') }}`.
- Within Java executors prefer `ExecutionContext.read()` rather than `get()` to ensure type safety and consumer cleanup.

## Custom functions
- Functions returning boolean values can be annotated with `@AviatorBooleanFunction` and are auto‑registered.
- Expose function names with an initial capital letter, e.g. `String.contains`.
- Call custom functions with the optional `fn:` prefix to avoid collisions with node keys: `{{ fn:String.contains('zenflow', 'flow') }}`.

## Evaluator cloning
- TemplateService keeps a preconfigured immutable evaluator. Obtain a mutable copy before registering extra functions:
  ```java
  var evaluator = context.getEvaluator().clone();
  evaluator.addFunction(new MyCustomFunction());
  ```

## Reference extraction
`TemplateService.extractRefs()` can scan strings, maps, or lists for `{{ }}` expressions and returns the referenced keys for dependency analysis.

## Summary
TemplateService provides a safe, extensible way to evaluate expressions, ensuring that custom functions and data lookups remain isolated per execution while keeping template syntax concise.
