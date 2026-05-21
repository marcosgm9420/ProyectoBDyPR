package hechizos;

import personaje.Personaje;

public abstract class HechizosBase implements Hechizo {

    // Atributos comunes a todos los hechizos 
    protected String nombre;   // Nombre que se muestra en pantalla
    protected int    coste;    // Energía que gasta al lanzarse

    // cualquier hechizo necesita al menos nombre y coste
    public HechizosBase(String nombre, int coste) {
        this.nombre = nombre;
        this.coste  = coste;
    }

    @Override
    public boolean lanzar(Personaje lanzador, Personaje objetivo) {
        // 1. Comprobar energía
        if (lanzador.energia < coste) {
            System.out.println("    Sin energía para " + nombre);
            return false;
        }

        // 2. Cobrar el coste de energía
        lanzador.energia -= coste;

        // 3. Cada subclase aplica su efecto específico
        aplicarEfecto(lanzador, objetivo);
        return true;
    }

    /*
     * Método abstracto. Cada hechizo concreto decide qué efecto produce: curar, dañar, infectar con un DoT, regenerar con un HoT
     * Aquí queda explicado quien lo lanza y quien lo recibe 
     */
    protected abstract void aplicarEfecto(Personaje lanzador, Personaje objetivo);

    // El nombre se gestiona aquí una sola vez para todos los hechizos
    @Override
    public String getNombre() {
        return nombre;
    }

    //sigue siendo abstracto: cada hechizo dice si daña o cura
    @Override
    public abstract boolean esOfensivo();
}

