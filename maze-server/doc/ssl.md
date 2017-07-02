# SSL/TLS konfigurieren

Der MazeNet-Server lässt sich sowohl unverschlüsselt als auch verschlüsselt ansprechen.
Standardmäßig ist die verschlüsselte Variante noch nicht konfiguriert.
Der Grund ist, dass der Server seinen private key kennen muss.
Also müsste man den private key des Servers mit raus geben, aber das ist etwas was man nie tun sollte.
Oder aber man müsste den Server irgendwo sicher hosten. Dies wird evtl in der Zukunft noch passieren.

Bis dahin muss man jedoch selber ein private/public key pair generieren. Dieses selbst generierte key pair hinterlegt man nun in seiner eigenen Serverinstanz.
In Java legt man dafür einen Keystore an, der dieses Keypaar enthält.

Nun muss man den Pfad zum Keystore und das dazu gehörige Passwort dem Server übergeben.
Dies geschieht in einer config-Datei die dem Server mitgegeben wird. Weiterführendes finden Sie unter [config.md](config.md)

Die relevaten Einträge in der Config-Datei sind folgende:
```
SSL_CERT_STORE=/home/karlHeinz/mykey.jks
SSL_CERT_STORE_PASSWD=SuPeRSeCrET8205
```

Damit ihr Client nun mit ihrer Serverinstanz kommunizieren kann, müssen Sie den public key exportieren und in einen Truststore für den Client packen. Dem Client muss dieser Truststore mit dem entsprechenden Passwort bekannt gemacht werden.

Nun kann sich Ihr Client mit Ihrer Serverinstanz verschlüsselt verbinden.
Für den entgültigen Wettbewerk muss sich Ihr Client natürlich zu unserer Serverinstanz verbinden, die ein anderes public/private key pair hat. Der public key daraus wird beim Server mitgeliefert und liegt als zusätzlich in den Truststore zu importierendes Zertifikat `public-key.crt` vor.
Ihr Truststore muss also bei Einsatz verschlüsselter Verbindungen mindestens zwei Einträge haben, einen für Ihr publicy key und einen für unseren. Bei Bedarf kann dieses Truststore natürlich beliebig erweitert werden, um z.B. eigenständig mit den Instanzen anderer Gruppen verschlüsselt in Wettbewerb zu treten.

## FAQ

* Warum ist dies keine Schritt-für-Schritt-Anleitung?

    Die einzelnen Schritte um eine verschlüsselte Verbindung einsetzen zu können, wurden in der entsprechenden Praktikumsaufgabe bereits in Eigenleistung erarbeitet und muss hier lediglich transferiert werden.

* Warum ist beim Server nicht bereits ein Keystore vorhanden?

    Da Sie eh ein eigenes public/private key pair erstellen müssen, ist es für Sie kein Mehraufwand den Keystore selber beim Generieren des key pairs mitzuerstellen. Zusätzlich ist es von Vorteil, dass Sie so besser wissen wie die Datei heißt, wo sie liegt, was ihr Zweck ist und so weiter.
