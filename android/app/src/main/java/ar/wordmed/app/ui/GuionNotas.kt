package ar.wordmed.app.ui

import ar.wordmed.app.datos.Nota

/**
 * Los cuadraditos de las notas, dibujados **adentro** del cuadernillo.
 *
 * Son un elemento más de la página, así que se mueven con el texto porque
 * *son* el texto: no hay nada que sincronizar con el scroll. Dibujarlos por
 * fuera, encima del WebView, no podía quedar pegado —el WebView scrollea en
 * su propio hilo y avisa después—, y de paso así crecen y se achican solos
 * con el zoom.
 *
 * **Cada nota está prendida a un elemento del cuadernillo**, no a una
 * proporción de la hoja. Los cuadernillos abren y cierran secciones y fichas;
 * cada vez que una crece, el documento se alarga, y una nota guardada como
 * fracción del largo se corría sola. Prendida al párrafo, se queda con su
 * párrafo: si lo que se despliega está más arriba, baja con él; si está más
 * abajo, no se mueve. Y si su párrafo queda plegado, desaparece con él.
 *
 * Se inyecta al terminar de cargar. No toca el archivo del cuadernillo.
 */
const val GUION_NOTAS = """
(function(){
  if (window.WordmedNotas) return;

  /* El cuadradito se ve chico pero se agarra grande: el elemento mide AGARRE
     y es transparente, y el dibujo lo pone un ::before de LADO centrado
     adentro. Así el dedo no tiene que acertarle justo. */
  var LADO = 20;      /* px CSS: en pantalla son unos 7 dp de dibujo */
  var AGARRE = 80;    /* la zona sensible: unos 29 dp, cuatro veces el dibujo */
  var MITAD = AGARRE / 2;
  var ESPERA = 200;   /* cuánto hay que sostener antes de poder arrastrar */
  var MARGEN = 80;    /* cuánto hay que acercarse al borde para que corra */

  var capa = document.createElement('div');
  capa.style.cssText = 'position:absolute;left:0;top:0;width:0;height:0';
  document.body.appendChild(capa);

  var estilo = document.createElement('style');
  estilo.textContent =
    '.wm-nota{position:absolute;background:transparent;touch-action:none;' +
    'width:' + AGARRE + 'px;height:' + AGARRE + 'px}' +
    '.wm-nota::before{content:"";position:absolute;box-sizing:border-box;' +
    'left:50%;top:50%;width:' + LADO + 'px;height:' + LADO + 'px;' +
    'margin:' + (-LADO / 2) + 'px 0 0 ' + (-LADO / 2) + 'px;' +
    'border:2px solid;border-image:linear-gradient(135deg,' +
    '#27AE60,#8E44AD,#2980B9,#D4AC0D,#008CBA,#C0392B) 1;' +
    'transition:transform .12s ease-out}' +
    /* Apenas se toca crece un poco: avisa que el dedo le pegó. Al quedar
       sostenido crece más, que es la señal de que ya se puede arrastrar. */
    '.wm-nota.wm-tocada::before{transform:scale(1.2)}' +
    '.wm-nota.wm-agarrada::before{transform:scale(1.5)}';
  document.head.appendChild(estilo);

  var notas = [];
  var marcas = [];
  var med = { ancho: 1, largo: 1, ox: 0, oy: 0 };

  /* La capa se esconde para medir: sus propios cuadraditos cuentan para el
     alto del documento, y medir con ellos puestos haría que una nota de
     abajo se empujara a sí misma hacia el fondo. */
  function medir(){
    capa.style.display = 'none';
    var ancho = document.documentElement.scrollWidth;
    var largo = document.documentElement.scrollHeight;
    capa.style.display = '';
    var r = capa.getBoundingClientRect();
    med = {
      ancho: ancho || 1,
      largo: largo || 1,
      ox: r.left + window.scrollX,
      oy: r.top + window.scrollY
    };
  }

  /* Dónde va la nota, en coordenadas del documento. Primero se busca su
     ancla; si no está —porque cambió el cuadernillo— se cae a la fracción,
     que es peor pero no la pierde. */
  function lugar(n){
    if (n.ancla) {
      var el = document.getElementById(n.ancla);
      if (el) {
        var r = el.getBoundingClientRect();
        /* Un ancla plegada no mide nada: la nota se esconde con ella. */
        if (!r.width && !r.height) return null;
        return { x: r.left + window.scrollX + n.dx, y: r.top + window.scrollY + n.dy };
      }
    }
    return { x: (n.fx || 0.5) * med.ancho, y: (n.fy || 0) * med.largo };
  }

  function colocar(el, n){
    var p = lugar(n);
    if (!p) { el.style.display = 'none'; return; }
    el.style.display = '';
    el.style.left = (p.x - med.ox - MITAD) + 'px';
    el.style.top  = (p.y - med.oy - MITAD) + 'px';
  }

  function reubicarTodas(){
    medir();
    for (var i = 0; i < marcas.length; i++) colocar(marcas[i], notas[i]);
  }

  /* De qué elemento del cuadernillo se prende una nota que quedó en (cx,cy)
     de la ventana. Se busca el ancestro con `id` más cercano —los
     cuadernillos numeran todas sus secciones— y se anota la distancia. */
  function prender(cx, cy){
    capa.style.pointerEvents = 'none';
    var bajo = document.elementFromPoint(cx, cy);
    capa.style.pointerEvents = '';
    var anc = bajo && bajo.closest ? bajo.closest('[id]') : null;
    if (!anc || !anc.id) return null;
    var r = anc.getBoundingClientRect();
    return {
      ancla: anc.id,
      dx: (cx + window.scrollX) - (r.left + window.scrollX),
      dy: (cy + window.scrollY) - (r.top + window.scrollY)
    };
  }

  function avisarMovida(n, el){
    var r = el.getBoundingClientRect();
    var cx = r.left + MITAD, cy = r.top + MITAD;
    var a = prender(cx, cy);
    window.WordmedApp.moverNota(
      n.id,
      (cx + window.scrollX) / med.ancho,
      (cy + window.scrollY) / med.largo,
      a ? a.ancla : '',
      a ? a.dx : 0,
      a ? a.dy : 0
    );
  }

  function preparar(el, n){
    var arrastrando = false, temporizador = null, bucle = null;
    var x0 = 0, y0 = 0, sy0 = 0, iz0 = 0, ar0 = 0, ux = 0, uy = 0;

    function reubicar(){
      el.style.left = (iz0 + (ux - x0)) + 'px';
      el.style.top  = (ar0 + (uy - y0) + (window.scrollY - sy0)) + 'px';
    }

    function soltarTodo(){
      if (temporizador) { clearTimeout(temporizador); temporizador = null; }
      if (bucle) { clearInterval(bucle); bucle = null; }
      el.classList.remove('wm-tocada');
    }

    el.addEventListener('pointerdown', function(e){
      x0 = ux = e.clientX; y0 = uy = e.clientY;
      sy0 = window.scrollY;
      iz0 = parseFloat(el.style.left) || 0;
      ar0 = parseFloat(el.style.top) || 0;
      el.classList.add('wm-tocada');
      temporizador = setTimeout(function(){
        arrastrando = true;
        el.classList.remove('wm-tocada');
        el.classList.add('wm-agarrada');
        try { el.setPointerCapture(e.pointerId); } catch(err){}
        /* Contra el borde la hoja se corre sola y la nota acompaña, para
           poder llevarla a una sección que no está a la vista. */
        bucle = setInterval(function(){
          var paso = 0;
          if (uy < MARGEN) paso = -14;
          else if (uy > window.innerHeight - MARGEN) paso = 14;
          if (paso) { window.scrollBy(0, paso); reubicar(); }
        }, 16);
      }, ESPERA);
    });

    el.addEventListener('pointermove', function(e){
      ux = e.clientX; uy = e.clientY;
      if (!arrastrando) {
        /* Si el dedo se va antes de tiempo era un scroll, no un agarre. */
        if (Math.abs(ux - x0) > 8 || Math.abs(uy - y0) > 8) soltarTodo();
        return;
      }
      e.preventDefault();
      reubicar();
    });

    el.addEventListener('pointerup', function(e){
      soltarTodo();
      if (arrastrando) {
        arrastrando = false;
        el.classList.remove('wm-agarrada');
        avisarMovida(n, el);
      } else if (Math.abs(e.clientX - x0) < 8 && Math.abs(e.clientY - y0) < 8) {
        window.WordmedApp.abrirNota(n.id);
      }
    });

    el.addEventListener('pointercancel', function(){
      soltarTodo();
      if (arrastrando) {
        arrastrando = false;
        el.classList.remove('wm-agarrada');
        colocar(el, n);
      }
    });
  }

  window.WordmedNotas = {
    pintar: function(lista){
      notas = lista || [];
      while (capa.firstChild) capa.removeChild(capa.firstChild);
      marcas = [];
      medir();
      for (var i = 0; i < notas.length; i++) {
        var el = document.createElement('div');
        el.className = 'wm-nota';
        capa.appendChild(el);
        marcas.push(el);
        colocar(el, notas[i]);
        preparar(el, notas[i]);
      }
    },
    crearEnElCentro: function(){
      medir();
      var cx = window.innerWidth / 2, cy = window.innerHeight / 2;
      var a = prender(cx, cy);
      window.WordmedApp.crearNota(
        (cx + window.scrollX) / med.ancho,
        (cy + window.scrollY) / med.largo,
        a ? a.ancla : '',
        a ? a.dx : 0,
        a ? a.dy : 0
      );
    }
  };

  /* El cuadernillo despliega y pliega secciones y fichas, y eso mueve todo
     lo que tiene debajo. Con el observador la nota acompaña a su párrafo
     cuadro a cuadro durante la animación, en vez de quedar flotando. Se
     reubica nada más: no se recrea, que haría parpadear el dibujo. */
  if (window.ResizeObserver) {
    new ResizeObserver(function(){ if (marcas.length) reubicarTodas(); })
      .observe(document.body);
  }
  window.addEventListener('resize', function(){ if (marcas.length) reubicarTodas(); });
  window.addEventListener('load', function(){ if (marcas.length) reubicarTodas(); });
})();
"""

private fun escapar(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

/** Las notas como arreglo de JavaScript, para pasárselas al guion. */
fun notasEnJs(notas: List<Nota>) = notas.joinToString(",", "[", "]") { n ->
    val ancla = n.ancla?.let { "\"${escapar(it)}\"" } ?: "null"
    """{"id":"${n.id}","ancla":$ancla,"dx":${n.dx},"dy":${n.dy},""" +
        """"fx":${n.fx ?: 0.5f},"fy":${n.fy ?: 0f}}"""
}
