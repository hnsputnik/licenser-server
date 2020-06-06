FROM golang:1.12 as builder 

WORKDIR /license-server

#### Cache Vendor ... 
COPY go.mod go.sum /license-server/

RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o /usr/local/bin/license-server .


######## Start a new stage from scratch #######
FROM alpine:latest
RUN apk update && apk add bash
RUN apk add --no-cache ca-certificates openssl

WORKDIR /license-server

COPY --from=builder /usr/local/bin/license-server .

CMD ["./license-server"] --v
