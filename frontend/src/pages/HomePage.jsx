import { useState } from 'react'

function HomePage() {
  const [longUrl, setLongUrl] = useState('')
  const [shortUrl, setShortUrl] = useState('')
  const [qrCodeUrl, setQrCodeUrl] = useState('')
  const [qrCodeError, setQrCodeError] = useState('')
  const [qrCodeLoading, setQrCodeLoading] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setShortUrl('')
    setLoading(true)

    try {
      const response = await fetch('http://localhost:8080/api/v1/create/shorten', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          originalUrl: longUrl,
          baseUrl: 'http://localhost:8080', // Use API Gateway URL for redirects
        }),
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.message || 'Failed to create short URL')
      }

      const data = await response.json()
      setShortUrl(data.shortUrl)
      setQrCodeError('')
      setQrCodeLoading(true)
      // Generate QR code URL
      if (data.shortUrl) {
        const qrCodeEndpoint = `http://localhost:8080/api/v1/create/qr?shortUrl=${encodeURIComponent(data.shortUrl)}`
        setQrCodeUrl(qrCodeEndpoint)
        console.log('QR Code URL:', qrCodeEndpoint)
        
        // Test if QR code endpoint is accessible
        fetch(qrCodeEndpoint)
          .then(res => {
            if (!res.ok) {
              throw new Error(`QR code endpoint returned ${res.status}`)
            }
            return res.blob()
          })
          .then(blob => {
            if (blob.type.startsWith('image/')) {
              console.log('QR code endpoint is working')
              setQrCodeError('')
            } else {
              throw new Error('Response is not an image')
            }
          })
          .catch(err => {
            console.error('QR code endpoint error:', err)
            setQrCodeError(`QR code unavailable: ${err.message}`)
          })
          .finally(() => {
            setQrCodeLoading(false)
          })
      }
      setLongUrl('')
    } catch (err) {
      setError(err.message || 'An error occurred. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const copyToClipboard = () => {
    navigator.clipboard.writeText(shortUrl)
    // You could add a toast notification here
    alert('Copied to clipboard!')
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50 flex items-center justify-center px-4">
      <div className="w-full max-w-2xl">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-5xl font-bold text-gray-900 mb-2">
            TinyURL
          </h1>
          <p className="text-gray-600 text-lg">
            Shorten your long URLs instantly
          </p>
        </div>

        {/* Main Card */}
        <div className="bg-white rounded-2xl shadow-xl p-8">
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label htmlFor="longUrl" className="block text-sm font-medium text-gray-700 mb-2">
                Enter your long URL
              </label>
              <input
                type="url"
                id="longUrl"
                value={longUrl}
                onChange={(e) => setLongUrl(e.target.value)}
                placeholder="https://www.example.com/very/long/url/path"
                required
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent outline-none transition-all"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-indigo-600 text-white py-3 px-6 rounded-lg font-semibold hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
            >
              {loading ? (
                <span className="flex items-center justify-center">
                  <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Creating...
                </span>
              ) : (
                'Shorten URL'
              )}
            </button>
          </form>

          {/* Error Message */}
          {error && (
            <div className="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-red-800 text-sm">{error}</p>
            </div>
          )}

          {/* Short URL Result */}
          {shortUrl && (
            <div className="mt-6 p-6 bg-green-50 border border-green-200 rounded-lg">
              <p className="text-sm font-medium text-gray-700 mb-4">Your short URL:</p>
              <div className="flex items-center gap-2 mb-4">
                <input
                  type="text"
                  value={shortUrl}
                  readOnly
                  className="flex-1 px-4 py-2 bg-white border border-green-300 rounded-lg text-indigo-600 font-mono text-sm focus:outline-none"
                />
                <button
                  onClick={copyToClipboard}
                  className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 transition-all"
                >
                  Copy
                </button>
              </div>
              
              {/* QR Code */}
              {qrCodeUrl && (
                <div className="mt-4 pt-4 border-t border-green-200">
                  <p className="text-sm font-medium text-gray-700 mb-3 text-center">QR Code:</p>
                  <div className="flex justify-center">
                    <div className="bg-white p-4 rounded-lg shadow-sm">
                      {qrCodeLoading && (
                        <div className="w-48 h-48 flex items-center justify-center">
                          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
                        </div>
                      )}
                      {!qrCodeLoading && !qrCodeError && (
                        <img 
                          src={qrCodeUrl} 
                          alt="QR Code" 
                          className="w-48 h-48"
                          onError={(e) => {
                            console.error('QR Code image failed to load:', qrCodeUrl)
                            setQrCodeError('Failed to load QR code image. Please ensure the create-service is running and has been rebuilt with QR code dependencies.')
                          }}
                          onLoad={() => {
                            console.log('QR Code image loaded successfully')
                            setQrCodeError('')
                          }}
                        />
                      )}
                      {qrCodeError && (
                        <div className="w-48 h-48 flex items-center justify-center">
                          <div className="text-red-500 text-xs text-center p-4">
                            {qrCodeError}
                            <br />
                            <span className="text-gray-400 text-xs mt-2 block">
                              Make sure create-service is running and rebuilt
                            </span>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                  {!qrCodeError && !qrCodeLoading && (
                    <p className="text-xs text-gray-500 text-center mt-2">Scan to open the short URL</p>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="text-center mt-8 text-gray-500 text-sm">
          <p>Fast, reliable, and free URL shortening service</p>
        </div>
      </div>
    </div>
  )
}

export default HomePage

