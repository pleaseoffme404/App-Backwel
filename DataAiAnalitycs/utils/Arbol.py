import os
from pathlib import Path


def imprimir_arbol(ruta_inicial, excluir_carpetas):
    ruta_base = str(ruta_inicial)

    for root, dirs, files in os.walk(ruta_base):
        dirs[:] = [d for d in dirs if d not in excluir_carpetas]
        rel_path = os.path.relpath(root, ruta_base)

        if rel_path == '.':
            nivel = 0
            nombre_carpeta = ruta_inicial.name
        else:
            nivel = rel_path.count(os.sep) + 1
            nombre_carpeta = os.path.basename(root)

        indentacion = ' ' * 4 * nivel
        print(f"{indentacion}📁 {nombre_carpeta}/")
        sub_indentacion = ' ' * 4 * (nivel + 1)
        for f in files:
            print(f"{sub_indentacion}📄 {f}")

if __name__ == "__main__":

    RAIZ_PROYECTO = Path(__file__).resolve().parent.parent

    carpetas_a_ignorar = {'venv', '.idea', '__pycache__', '.git', 'node_modules', '.vscode'}

    print("=" * 50)
    print(f" Árbol del Proyecto: {RAIZ_PROYECTO.name}")
    print("=" * 50 + "\n")

    imprimir_arbol(RAIZ_PROYECTO, carpetas_a_ignorar)