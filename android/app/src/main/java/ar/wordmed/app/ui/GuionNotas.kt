package ar.wordmed.app.ui

import ar.wordmed.app.datos.Nota

/**
 * Los cuadraditos de las notas, dibujados **adentro** del cuadernillo.
 *
 * Antes los dibujaba Compose encima del WebView y se los movía a mano
 * siguiendo el scroll. Eso nunca puede quedar pegado: el WebView scrollea
 * en su propio hilo y avisa después, así que la marca iba siempre un paso
 * atrás, se despegaba en los tirones rápidos y flotaba en el rebote del
 * final. Siendo un elemento más de la página no hay nada que sincronizar
 * —se mueve con el texto porque *es* el texto— y de paso crece y se achica
 * sola con el zoom, sin que nadie haga cuentas.
 *
 * Se inyecta al terminar de cargar. No toca el archivo del cuadernillo: los
 * cuadraditos existen sólo mientras la página está abierta en la app.
 */
const val GUION_NOTAS = """
(function(){
  if (window.WordmedNotas) return;

  var LADO = 20;      /* px CSS: al encogerse la página quedan unos 14 dp */
  var MITAD = LADO / 2;
  var MARGEN = 80;    /* cuánto hay que acercarse al borde para que corra */

  var capa = document.createElement('div');
  capa.style.cssText = 'position:absolute;left:0;top:0;width:0;height:0';
  document.body.appendChild(capa);

  var estilo = document.createElement('style');
  estilo.textContent =
    '.wm-nota{position:absolute;box-sizing:border-box;' +
    'width:' + LADO + 'px;height:' + LADO + 'px;' +
    'border:2px solid;border-image:linear-gradient(135deg,' +
    '#27AE60,#8E44AD,#2980B9,#D4AC0D,#008CBA,#C0392B) 1;' +
    'background:transparent;touch-action:none;z-index:55}' +
    '.wm-nota.wm-agarrada{opacity:.55}';
  document.head.appendChild(estilo);

  var notas = [];
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

  function colocar(el, n){
    el.style.left = (n.fx * med.ancho - med.ox - MITAD) + 'px';
    el.style.top  = (n.fy * med.largo - med.oy - MITAD) + 'px';
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
    }

    el.addEventListener('pointerdown', function(e){
      x0 = ux = e.clientX; y0 = uy = e.clientY;
      sy0 = window.scrollY;
      iz0 = parseFloat(el.style.left) || 0;
      ar0 = parseFloat(el.style.top) || 0;
      temporizador = setTimeout(function(){
        arrastrando = true;
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
      }, 380);
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
        var fx = (parseFloat(el.style.left) + MITAD + med.ox) / med.ancho;
        var fy = (parseFloat(el.style.top) + MITAD + med.oy) / med.largo;
        window.WordmedApp.moverNota(n.id, fx, fy);
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

  function repintar(){
    while (capa.firstChild) capa.removeChild(capa.firstChild);
    medir();
    for (var i = 0; i < notas.length; i++) {
      var el = document.createElement('div');
      el.className = 'wm-nota';
      capa.appendChild(el);
      colocar(el, notas[i]);
      preparar(el, notas[i]);
    }
  }

  window.WordmedNotas = {
    pintar: function(lista){ notas = lista || []; repintar(); },
    crearEnElCentro: function(){
      medir();
      window.WordmedApp.crearNota(
        (window.scrollX + window.innerWidth / 2) / med.ancho,
        (window.scrollY + window.innerHeight / 2) / med.largo
      );
    }
  };

  /* El alto del documento cambia al girar el teléfono o al mover el tamaño
     de letra, y con él la posición de cada nota. */
  var pendiente = null;
  window.addEventListener('resize', function(){
    if (pendiente) clearTimeout(pendiente);
    pendiente = setTimeout(repintar, 120);
  });
})();
"""

/** Las notas como arreglo de JavaScript, para pasárselas al guion. */
fun notasEnJs(notas: List<Nota>) = notas
    .filter { it.fx != null && it.fy != null }
    .joinToString(",", "[", "]") { """{"id":"${it.id}","fx":${it.fx},"fy":${it.fy}}""" }
