export async function onRequest(context) {
  const { request, env } = context;
  const origen = new URL(request.url);
  const destino = new URL(origen.pathname + origen.search, env.BACKEND_URL);
  return fetch(destino.toString(), request);
}
