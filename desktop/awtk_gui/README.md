# AWTK-MINIJVM

[AWTK](https://github.com/zlgopen/awtk) bind to [MINIJVM](https://github.com/digitalgust/miniJVM)   

## Prepare
   Clone miniJVM first   
   Clone awtk into miniJVM/desktop/awtk   
   Clone awtk-minijvm into miniJVM/desktop/awtk_gui   

```
git clone https://github.com/digitalgust/miniJVM.git
cd desktop
git clone https://github.com/zlgopen/awtk.git
git clone https://github.com/zlgopen/awtk-minijvm.git awtk_gui
```

## Build

Compile AWTK

```
cd awtk; scons; cd -
```

> AWTK manual : https://github.com/zlgopen/awtk

Build AWTK-MINIJVM

```
make
```

