package com.hyperdeck.shizuku;

interface IShellService {
    void destroy() = 16777114;
    String execute(String command) = 1;
    void cancel() = 2;
}
