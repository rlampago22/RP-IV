# Classpath do nucleo (mvp/): as classes compiladas + o driver JDBC do banco.
# Uso nos outros scripts:  . (Join-Path $PSScriptRoot 'dependencias.ps1'); $cp = Get-ClasspathMvp $saida
#
# O driver H2 e a MESMA versao que o backend/ usa (Spring Boot 3.4.5), para os dois processos
# compartilharem o arquivo backend/data/usina (AUTO_SERVER). Procura nesta ordem: mvp/lib/, o
# repositorio local do Maven (~/.m2, preenchido quando o backend e compilado) e, por ultimo, baixa do
# Maven Central para mvp/lib/ (ignorado pelo git) conferindo o SHA-256.
# O driver do Postgres so entra quando USINA_DB_URL aponta para jdbc:postgresql.

$script:DriversMvp = @(
    @{
        Nome    = 'H2'
        Grupo   = 'com/h2database'
        Artefato = 'h2'
        Versao  = '2.3.232'
        Sha256  = '8dae62d22db8982c3dcb3826edb9c727c5d302063a67eef7d63d82de401f07d3'
        Sempre  = $true
    },
    @{
        Nome    = 'PostgreSQL'
        Grupo   = 'org/postgresql'
        Artefato = 'postgresql'
        Versao  = '42.7.5'
        Sha256  = '69020b3bd20984543e817393f2e6c01a890ef2e37a77dd11d6d8508181d079ab'
        Sempre  = $false
    }
)

function Get-DriverMvp {
    param([hashtable]$Driver)

    $nomeJar = "$($Driver.Artefato)-$($Driver.Versao).jar"
    $pastaLib = Join-Path $PSScriptRoot 'lib'
    $noLib = Join-Path $pastaLib $nomeJar
    if (Test-Path -LiteralPath $noLib) { return $noLib }

    $noMaven = Join-Path $HOME (".m2/repository/$($Driver.Grupo)/$($Driver.Artefato)/$($Driver.Versao)/$nomeJar")
    if (Test-Path -LiteralPath $noMaven) { return $noMaven }

    New-Item -ItemType Directory -Force -Path $pastaLib | Out-Null
    $url = "https://repo1.maven.org/maven2/$($Driver.Grupo)/$($Driver.Artefato)/$($Driver.Versao)/$nomeJar"
    Write-Host "Baixando o driver $($Driver.Nome) ($nomeJar) do Maven Central..." -ForegroundColor Cyan
    $temporario = "$noLib.baixando"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $url -OutFile $temporario -UseBasicParsing
    $hash = (Get-FileHash -LiteralPath $temporario -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($hash -ne $Driver.Sha256) {
        Remove-Item -LiteralPath $temporario -Force
        throw "O $nomeJar baixado nao confere com o SHA-256 esperado; download descartado."
    }
    Move-Item -LiteralPath $temporario -Destination $noLib -Force
    return $noLib
}

function Get-ClasspathMvp {
    param([string]$PastaClasses)

    $usaPostgres = "$env:USINA_DB_URL".StartsWith('jdbc:postgresql')
    $partes = @($PastaClasses)
    foreach ($driver in $script:DriversMvp) {
        if ($driver.Sempre -or $usaPostgres) {
            $partes += Get-DriverMvp $driver
        }
    }
    return ($partes -join [IO.Path]::PathSeparator)
}
