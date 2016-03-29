switch (request.method) {
    case 'GET':
        out.println '{"method":"GET"}'
        break
    case 'POST':
        out.println '{"method":"POST"}'
        break
}
