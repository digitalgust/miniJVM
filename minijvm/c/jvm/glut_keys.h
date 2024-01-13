#pragma once

#include <GL/freeglut_std.h>

#define KEY_0 11
#define KEY_1 2
#define KEY_2 3
#define KEY_3 4
#define KEY_4 5
#define KEY_5 6
#define KEY_6 7
#define KEY_7 8
#define KEY_8 9
#define KEY_9 10
#define KEY_A 30
#define KEY_ADD 78
#define KEY_APOSTROPHE 40
#define KEY_APPS 221
#define KEY_AT 145
#define KEY_AX 150
#define KEY_B 48
#define KEY_BACK 14
#define KEY_BACKSLASH 43
#define KEY_C 46
#define KEY_CAPITAL 58
#define KEY_CIRCUMFLEX 144
#define KEY_CLEAR 218
#define KEY_COLON 146
#define KEY_COMMA 51
#define KEY_CONVERT 121
#define KEY_D 32
#define KEY_DECIMAL 83
#define KEY_DELETE 211
#define KEY_DIVIDE 181
#define KEY_DOWN 208
#define KEY_E 18
#define KEY_END 207
#define KEY_EQUALS 13
#define KEY_ESCAPE 1
#define KEY_F 33
#define KEY_F1 59
#define KEY_F10 68
#define KEY_F11 87
#define KEY_F12 88
#define KEY_F13 100
#define KEY_F14 101
#define KEY_F15 102
#define KEY_F16 103
#define KEY_F17 104
#define KEY_F18 105
#define KEY_F19 113
#define KEY_F2 60
#define KEY_F3 61
#define KEY_F4 62
#define KEY_F5 63
#define KEY_F6 64
#define KEY_F7 65
#define KEY_F8 66
#define KEY_F9 67
#define KEY_FUNCTION 196
#define KEY_G 34
#define KEY_GRAVE 41
#define KEY_H 35
#define KEY_HOME 199
#define KEY_I 23
#define KEY_INSERT 210
#define KEY_J 36
#define KEY_K 37
#define KEY_KANA 112
#define KEY_KANJI 148
#define KEY_L 38
#define KEY_LBRACKET 26
#define KEY_LCONTROL 29
#define KEY_LEFT 203
#define KEY_LMENU 56
#define KEY_LMETA 219
#define KEY_LSHIFT 42
#define KEY_LWIN 219
#define KEY_M 50
#define KEY_MINUS 12
#define KEY_MULTIPLY 55
#define KEY_N 49
#define KEY_NEXT 209
#define KEY_NOCONVERT 123
#define KEY_NONE 0
#define KEY_NUMLOCK 69
#define KEY_NUMPAD0 82
#define KEY_NUMPAD1 79
#define KEY_NUMPAD2 80
#define KEY_NUMPAD3 81
#define KEY_NUMPAD4 75
#define KEY_NUMPAD5 76
#define KEY_NUMPAD6 77
#define KEY_NUMPAD7 71
#define KEY_NUMPAD8 72
#define KEY_NUMPAD9 73
#define KEY_NUMPADCOMMA 179
#define KEY_NUMPADENTER 156
#define KEY_NUMPADEQUALS 141
#define KEY_O 24
#define KEY_P 25
#define KEY_PAUSE 197
#define KEY_PERIOD 52
#define KEY_POWER 222
#define KEY_PRIOR 201
#define KEY_Q 16
#define KEY_R 19
#define KEY_RBRACKET 27
#define KEY_RCONTROL 157
#define KEY_RETURN 28
#define KEY_RIGHT 205
#define KEY_RMENU 184
#define KEY_RMETA 220
#define KEY_RSHIFT 54
#define KEY_RWIN 220
#define KEY_S 31
#define KEY_SCROLL 70
#define KEY_SECTION 167
#define KEY_SEMICOLON 39
#define KEY_SLASH 53
#define KEY_SLEEP 223
#define KEY_SPACE 57
#define KEY_STOP 149
#define KEY_SUBTRACT 74
#define KEY_SYSRQ 183
#define KEY_T 20
#define KEY_TAB 15
#define KEY_U 22
#define KEY_UNDERLINE 147
#define KEY_UNLABELED 151
#define KEY_UP 200
#define KEY_V 47
#define KEY_W 17
#define KEY_X 45
#define KEY_Y 21
#define KEY_YEN 125
#define KEY_Z 44
#define KEYBOARD_SIZE 256

#define SPECIAL_GLUT_KEY_OFFSET 0xFF

#if 0
const inline int lwjgl2glut[] = {
  [KEY_0] = '0',
  [KEY_1] = '1',
  [KEY_2] = '2',
  [KEY_3] = '3',
  [KEY_4] = '4',
  [KEY_5] = '5',
  [KEY_6] = '6',
  [KEY_7] = '7',
  [KEY_8] = '8',
  [KEY_9] = '9',
  [KEY_A] = 'A',
  [KEY_ADD] = '+',
  [KEY_APOSTROPHE] = '\'',
  [KEY_APPS] = -1,
  [KEY_AT] = '@',
  [KEY_AX] = -1,
  [KEY_B] = 'B',
  [KEY_BACK] = 8,
  [KEY_BACKSLASH] = '\\',
  [KEY_C] = 'C',
  [KEY_CAPITAL] = -1,
  [KEY_CIRCUMFLEX] = -1,
  [KEY_CLEAR] = -1,
  [KEY_COLON] = ':',
  [KEY_COMMA] = ',',
  [KEY_CONVERT] = -1,
  [KEY_D] = 'D',
  [KEY_DECIMAL] = -1,
  [KEY_DELETE] = 127,
  [KEY_DIVIDE] = -1,
  [KEY_DOWN] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_DOWN),
  [KEY_E] = 'E',
  [KEY_END] = -1,
  [KEY_EQUALS] = '=',
  [KEY_ESCAPE] = 27,
  [KEY_F] = 'F',
  [KEY_F1] = KEY_F1,
  [KEY_F10] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F1),
  [KEY_F11] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F11),
  [KEY_F12] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F12),
  [KEY_F13] = -1,
  [KEY_F14] = -1,
  [KEY_F15] = -1,
  [KEY_F16] = -1,
  [KEY_F17] = -1,
  [KEY_F18] = -1,
  [KEY_F19] = -1,
  [KEY_F2] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F2),
  [KEY_F3] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F3),
  [KEY_F4] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F4),
  [KEY_F5] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F5),
  [KEY_F6] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F6),
  [KEY_F7] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F7),
  [KEY_F8] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F8),
  [KEY_F9] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F9),
  [KEY_FUNCTION] = -1,
  [KEY_G] = 'G',
  [KEY_GRAVE] = '`',
  [KEY_H] = 'H',
  [KEY_HOME] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_HOME),
  [KEY_I] = 'I',
  [KEY_INSERT] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_INSERT),
  [KEY_J] = 'J',
  [KEY_K] = 'K',
  [KEY_KANA] = -1,
  [KEY_KANJI] = -1,
  [KEY_L] = 'L',
  [KEY_LBRACKET] = '[',
  [KEY_LCONTROL] = -1,
  [KEY_LEFT] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_LEFT),
  [KEY_LMENU] = -1,
  [KEY_LMETA] = -1,
  [KEY_LSHIFT] = -1,
  // [KEY_LWIN] = -1,
  [KEY_M] = 'M',
  [KEY_MINUS] = '-',
  [KEY_MULTIPLY] = '*',
  [KEY_N] = 'N',
  [KEY_NEXT] = -1,
  [KEY_NOCONVERT] = -1,
  [KEY_NONE] = -1,
  [KEY_NUMLOCK] = -1,
  [KEY_NUMPAD0] = -1,
  [KEY_NUMPAD1] = -1,
  [KEY_NUMPAD2] = -1,
  [KEY_NUMPAD3] = -1,
  [KEY_NUMPAD4] = -1,
  [KEY_NUMPAD5] = -1,
  [KEY_NUMPAD6] = -1,
  [KEY_NUMPAD7] = -1,
  [KEY_NUMPAD8] = -1,
  [KEY_NUMPAD9] = -1,
  [KEY_NUMPADCOMMA] = -1,
  [KEY_NUMPADENTER] = -1,
  [KEY_NUMPADEQUALS] = -1,
  [KEY_O] = 'O',
  [KEY_P] = 'P',
  [KEY_PAUSE] = -1,
  [KEY_PERIOD] = '.',
  [KEY_POWER] = -1,
  [KEY_PRIOR] = -1,
  [KEY_Q] = 'Q',
  [KEY_R] = 'R',
  [KEY_RBRACKET] = ']',
  [KEY_RCONTROL] = -1,
  [KEY_RETURN] = '\r',
  [KEY_RIGHT] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_RIGHT),
  [KEY_RMENU] = -1,
  [KEY_RMETA] = -1,
  [KEY_RSHIFT] = -1,
  // [KEY_RWIN] = -1,
  [KEY_S] = 'S',
  [KEY_SCROLL] = -1,
  [KEY_SECTION] = -1,
  [KEY_SEMICOLON] = ';',
  [KEY_SLASH] = '/',
  [KEY_SLEEP] = -1,
  [KEY_SPACE] = ' ',
  [KEY_STOP] = -1,
  [KEY_SUBTRACT] = '-',
  [KEY_SYSRQ] = -1,
  [KEY_T] = 'T',
  [KEY_TAB] = '\t',
  [KEY_U] = 'U',
  [KEY_UNDERLINE] = '_',
  [KEY_UNLABELED] = -1,
  [KEY_UP] = (SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_UP),
  [KEY_V] = 'V',
  [KEY_W] = 'W',
  [KEY_X] = 'X',
  [KEY_Y] = 'Y',
  [KEY_YEN] = -1,
  [KEY_Z] = 'Z',
};
#endif

const inline short glut2lwjgl[] = {
    ['0'] = KEY_0,
    ['1'] = KEY_1,
    ['2'] = KEY_2,
    ['3'] = KEY_3,
    ['4'] = KEY_4,
    ['5'] = KEY_5,
    ['6'] = KEY_6,
    ['7'] = KEY_7,
    ['8'] = KEY_8,
    ['9'] = KEY_9,
    ['A'] = KEY_A,
    ['+'] = KEY_ADD,
    ['\''] = KEY_APOSTROPHE,
    ['@'] = KEY_AT,
    ['B'] = KEY_B,
    [8] = KEY_BACK,
    ['\\'] = KEY_BACKSLASH,
    ['C'] = KEY_C,
    [':'] = KEY_COLON,
    [','] = KEY_COMMA,
    ['D'] = KEY_D,
    [127] = KEY_DELETE,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_DOWN)] = KEY_DOWN,
    ['E'] = KEY_E,
    ['='] = KEY_EQUALS,
    [27] = KEY_ESCAPE,
    ['F'] = KEY_F,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F1)] = KEY_F1,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F10)] = KEY_F10,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F11)] = KEY_F11,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F12)] = KEY_F12,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F2)] = KEY_F2,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F3)] = KEY_F3,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F4)] = KEY_F4,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F5)] = KEY_F5,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F6)] = KEY_F6,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F7)] = KEY_F7,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F8)] = KEY_F8,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_F9)] = KEY_F9,
    ['G'] = KEY_G,
    ['`'] = KEY_GRAVE,
    ['H'] = KEY_H,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_HOME)] = KEY_HOME,
    ['I'] = KEY_I,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_INSERT)] = KEY_INSERT,
    ['J'] = KEY_J,
    ['K'] = KEY_K,
    ['L'] = KEY_L,
    ['['] = KEY_LBRACKET,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_LEFT)] = KEY_LEFT,
    ['M'] = KEY_M,
    ['-'] = KEY_MINUS,
    ['*'] = KEY_MULTIPLY,
    ['N'] = KEY_N,
    ['O'] = KEY_O,
    ['P'] = KEY_P,
    ['.'] = KEY_PERIOD,
    ['Q'] = KEY_Q,
    ['R'] = KEY_R,
    [']'] = KEY_RBRACKET,
    ['\r'] = KEY_RETURN,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_RIGHT)] = KEY_RIGHT,
    ['S'] = KEY_S,
    [';'] = KEY_SEMICOLON,
    ['/'] = KEY_SLASH,
    [' '] = KEY_SPACE,
    ['T'] = KEY_T,
    ['\t'] = KEY_TAB,
    ['U'] = KEY_U,
    ['_'] = KEY_UNDERLINE,
    [(SPECIAL_GLUT_KEY_OFFSET + GLUT_KEY_UP)] = KEY_UP,
    ['V'] = KEY_V,
    ['W'] = KEY_W,
    ['X'] = KEY_X,
    ['Y'] = KEY_Y,
    ['Z'] = KEY_Z,
};
