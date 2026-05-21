package personaje;
 
import java.util.ArrayList;
import armas.Arma;
import estado.Estado;
import hechizos.Hechizo;
 
public class Personaje {

    // ATRIBUTOS 

    protected String nombre;       // 'protected' = visible en subclases (Mercenario, Doctor...)
    protected int vidaActual;      //   pero NO desde fuera del paquete. Más seguro que 'public'
    protected int vidaMax;
    public int energia;            // 'public' porque los hechizos acceden a ella directamente
    protected Arma arma;
    protected ArrayList<Estado>  estados;  // estados activos ahora mismo (DoT, HoT...)
    protected ArrayList<Hechizo> hechizo;  // hechizos que conoce este personaje
    protected boolean estaCerca;           // controla si puede usar armas cuerpo a cuerpo
 
    // CONSTRUCTOR

    // el this: distingue el atributo del parámetro cuando tienen el mismo nombre
    public Personaje(String nombre, int vida, int energia, Arma arma) {
        this.nombre     = nombre;
        this.vidaMax    = vida;
        this.vidaActual = vida;    // al crear el personaje empieza con la vida al máximo
        this.energia    = energia;
        this.arma       = arma;
        this.estados    = new ArrayList<>();  // listas vacías, se rellenan durante el juego
        this.hechizo    = new ArrayList<>();
        this.estaCerca  = false;              // por defecto empieza lejos
    }
 
    //  MÉTODOS DE COMBATE 

    /**
     * Intenta atacar con el arma principal.
     * Devuelve false si el arma es cuerpo a cuerpo y el personaje está lejos.
     * Las subclases usan el false para decidir qué hacer
     */
    public boolean intentarAtacarConArma(Personaje objetivo) {
        // GUARD CLAUSE: si no se puede atacar, salimos pronto devolviendo false
        if (arma.esCuerpoACuerpo && !estaCerca) {
            System.out.println("    " + nombre + " está demasiado lejos para usar ["
                    + arma.getNombre() + "]!");
            return false;
        }
        int daño = arma.calcularDaño();
        System.out.println("    " + nombre + " ataca con ["
                + arma.getNombre() + "] a " + objetivo.getNombre()
                + " -> " + daño + " daño");
        objetivo.recibirDaño(daño);
        return true; 
    }

    /**
     * Equivalente a intentarAtacarConArma() pero para magia.
     * En vez de escribir hechizo.get(i).lanzar(this, objetivo) cada vez,
     * centralizamos aquí la llamada. 'this' es siempre el lanzador.
     *   físico: intentarAtacarConArma(objetivo)
     *   mágico: lanzarHechizo(h, objetivo)      
     */
    public boolean lanzarHechizo(Hechizo h, Personaje objetivo) {
        return h.lanzar(this, objetivo); // this = quien lanza, objetivo = quien recibe
    }
 
    // VIDA 
    public void recibirDaño(int cantidad) {
        vidaActual -= cantidad;
        if (vidaActual < 0) vidaActual = 0;  // nunca puede bajar de 0 ( no se puede tener la vida en negativo )
        System.out.println("      " + nombre + " HP: " + vidaActual + "/" + vidaMax);
    }
 
    public void curar(int cantidad) {
        vidaActual += cantidad;
        if (vidaActual > vidaMax) vidaActual = vidaMax;  // nunca supera el máximo
        System.out.println("      " + nombre + " HP: " + vidaActual + "/" + vidaMax);
    }
 
    // ¡ESTADOS (DOT / HOT) 

       public void agregarEstado(Estado nuevo) {
        for (Estado e : estados) {
            if (e.nombre.equals(nuevo.nombre)) {
                e.duracion = nuevo.duracion;  // reinicia el contador, no duplica
                System.out.println("      Efecto '" + nuevo.nombre + "' renovado en " + nombre);
                return;
            }
        }
        estados.add(nuevo);  // si no existía, lo añade nuevo
    }
 
    /**
     * Aplica los efectos activos (venenos, regeneraciones) y elimina los que han expirado.
     * El bucle va al REVÉS (size-1 hasta 0) para poder hacer remove(i)
     * sin que se descuadren los índices. Si fuera hacia adelante y borras el índice 1,
     * el elemento que estaba en el 2 pasa al 1 y te lo saltas.
     */
    public void procesarEstados() {
        for (int i = estados.size() - 1; i >= 0; i--) {
            Estado e = estados.get(i);
            e.aplicarEfecto(this);   // daña o cura según el tipo de estado
            if (e.haTerminado()) {   // si duracion <= 0, el efecto se ha agotado
                System.out.println("      Efecto '" + e.nombre + "' terminó en " + nombre);
                estados.remove(i);   // safe: borrar al revés no afecta a los índices anteriores
            }
        }
    }
 
  
    public void actuar(ArrayList<Personaje> enemigos, ArrayList<Personaje> aliados) {}
 
    // GETTERS 
    public boolean estaVivo()      { return vidaActual > 0; }  // muerto = 0 HP exactamente
    public String  getNombre()     { return nombre; }
    public int     getVidaActual() { return vidaActual; }
    public int     getVidaMax()    { return vidaMax; }
 
    // SETTERS 
    public void setVidaActual(int vidaActual) {
        // Math.max/min garantizan que el valor esté siempre entre 0 y vidaMax
        // Evita que un dato corrupto de la BD rompa el juego
        this.vidaActual = Math.max(0, Math.min(vidaActual, vidaMax));
    }
 
    public void setEnergia(int energia) {
        this.energia = energia;
    }
 
    /**
     * Devuelve el estado del personaje como una sola línea de texto formateada.
     * String.format con "%-20s" alinea el nombre a la izquierda en 20 caracteres,
     * así todos los personajes quedan en columnas al imprimirlos en pantalla.
     */
    public String getEstado() {
        String efecto    = estados.isEmpty() ? "" : " [" + estados.get(0).nombre + "]";
        String distancia = estaCerca ? "(cerca)" : "(lejos)";
        return String.format("%-20s HP: %3d/%-3d  EN: %3d  %s%s",
                nombre, vidaActual, vidaMax, energia, distancia, efecto);
    }
}