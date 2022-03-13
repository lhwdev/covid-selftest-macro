import { join } from "https://deno.land/std@0.128.0/path/mod.ts";

type Options = {
  cmd: string[];
  context?: ExecContext;
};

type ExecContextParam = {
  cwd?: string;
};

export function executeAsync(options: Options | string[]): Process {
  if (Array.isArray(options)) {
    return executeAsync({ cmd: options });
  }

  const process = Deno.run({
    cmd: [...options?.cmd],
    cwd: options?.context?.cwd,
  });

  return new Process(options, process);
}

export function execContext(context: ExecContextParam): ExecContext {
  return new ExecContext(context);
}

export const exec = execContext({});

export class Process {
  constructor(private options: Options, private process: Deno.Process) {}

  async join(): Promise<number> {
    const status = await this.process.status();
    if (!status.success) {
      throw new Error(
        `Process ${this.options.cmd} failed with code ${status.code}`,
      );
    }
    return status.code;
  }
}

export class ExecContext {
  constructor(private param: ExecContextParam) {}

  cd(path: string): ExecContext {
    return new ExecContext({
      ...this.param,
      cwd: join(this.param.cwd ?? ".", path),
    });
  }

  executeAsync(options: Options | string[]): Process {
    if (Array.isArray(options)) {
      return executeAsync({ cmd: options, context: this });
    } else {
      return executeAsync({ ...options, context: this });
    }
  }

  execute(options: Options | string[]): Promise<number> {
    return this.executeAsync(options).join();
  }

  get cwd(): string {
    return this.param.cwd ?? Deno.cwd();
  }
}
