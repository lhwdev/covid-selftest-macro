import { ensureDirSync } from "https://deno.land/std@0.128.0/fs/ensure_dir.ts";
import { join, resolve } from "https://deno.land/std@0.128.0/path/mod.ts";

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

  console.log(options.context?.cwd);

  const process = Deno.run({
    cmd: [...options.cmd],
    cwd: options.context?.cwd,
  });

  return new Process(options, process);
}

export function execContext(context: ExecContextParam): ExecContext {
  return new ExecContext(context);
}

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
    const newCwd = join(resolve(this.param.cwd ?? "."), path);
    ensureDirSync(newCwd);
    return new ExecContext({
      ...this.param,
      cwd: newCwd,
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

export const exec = execContext({});
